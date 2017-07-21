package com.example.tomas.speechprocessingapp.SpeechProcessing;

/**
 * Created by TOMAS on 29/03/2017.
 */

import android.os.Environment;
import android.util.Log;

import com.example.tomas.speechprocessingapp.SpeechProcessing.tools.Matrix;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.copyOfRange;

public class Speaker_Adaptation {

    private Matrix priors = null;//Preloaded weights
    private Matrix centres = null;//Preloaded Mean vector
    private Matrix covars = null;//Preloaded diagonal covariance matrix
    private Matrix featmat = null;//Feature matrix
    private int ngauss = 0;//Number of Gaussian Components
    private int samples = 0;//Number of samples
    private int nfeats = 0;//Number of features
    private Matrix ParamzScore = null;//Preloaded mean and standard deviation for standarization
    private Matrix pr = null;//New statistics for MAP adaptation
    private double[] ni = null;//Sum of PR
    private Matrix Normfeatmat = null; //Standarized feature matrix
    Matrix newpriors = null;
    Matrix newmeanvec = null;
    Matrix newcovars = null;



    /** Load UBM parameters and set feature matrix
     * @param xlist    List containing the feature vectors. Rows:samples; Columns:features
     */
    public Speaker_Adaptation(List<float[]> xlist) {

        featureExtraction fext = new featureExtraction();
        nfeats = fext.getNumberOfFeatures();
        double[] x = featstoarray(xlist,nfeats);

        //Get parameters of the UBM stored in the phone
        File sdcard = Environment.getExternalStorageDirectory();

        //Get the text file
        String UBMtype = "voiced";
        File file = new File(sdcard,"AppSpeechData"+ File.separator+"UBM"+File.separator+"UBM_priors_"+UBMtype+"2.txt");
        List<Float> vals = readFileIntoArray(file);
        double[] pr = listtoarray(vals);
        //
        file = new File(sdcard,"AppSpeechData"+ File.separator+"UBM"+File.separator+"UBM_means_"+UBMtype+"2.txt");
        vals = readFileIntoArray(file);
        double[] means = listtoarray(vals);
        //
        file = new File(sdcard,"AppSpeechData"+ File.separator+"UBM"+File.separator+"UBM_covars_"+UBMtype+"2.txt");
        vals = readFileIntoArray(file);
        double[] cov = listtoarray(vals);
        //Zscore MU and SIGMA for standarization of features
        file = new File(sdcard,"AppSpeechData"+ File.separator+"UBM"+File.separator+"UBM_zscore_"+UBMtype+".txt");
        vals = readFileIntoArray(file);
        double[] MU_SIG = listtoarray(vals);
        //
        //file = new File(sdcard,"AppSpeechData"+ File.separator+"UBM"+File.separator+"voiced_s1_pataka.txt");
        //vals = readFileIntoArray(file);
        //double[] feats = listtoarray(vals);


        ngauss = means.length/nfeats;

        featmat = new Matrix(x,x.length/nfeats);//Feature vector as matrix
        samples = featmat.getRowDimension();
        //featmat = new Matrix(feats,feats.length/nfeats);//Feature vector as matrix
        priors = new Matrix(pr, 1);//Priors from the UBM
        centres = new Matrix(means, ngauss);//Mean vector from the UBM
        covars = new Matrix(cov, ngauss);//Covariance matrix from the UBM
        ParamzScore = new Matrix(MU_SIG,2);//Mu and SIGMA from zscore are in the same txt file
    }

    //Compute Bhattacharyya distance
    public double spkrdist()
    {
        double Bha = 0;
        gmmadap();

        Matrix dm = newmeanvec.minus(centres); //(u2-u1)
        double[] distprior = new double[ngauss];
        double[] distmean = new double[ngauss];
        double[] distcov = new double[ngauss];

        for(int igauss=0;igauss<ngauss;igauss++)
        {

            //Compute distance between weights
            //log(w1*w2)
            distprior[igauss] = Math.log(priors.A[0][igauss]*newpriors.A[0][igauss]);

            //Compute distance between mean vectors
            //(u2-u1)'([(Cov2+Cov1)/2]^-1)(u2-u1)
            Matrix dcov1 = new Matrix(covars.A[igauss],1);//Covariance from UBM
            Matrix dcov2 = new Matrix(newcovars.A[igauss],1);//Covariance from Speaker
            Matrix sumCov = dcov2.plus(dcov1);//cov2+cov1
            sumCov.timesEquals(0.5);//(cov2+cov1)/2
            Matrix dmtemp = new Matrix(dm.A[igauss],1);
            Matrix tempCov = dmtemp.arrayRightDivide(sumCov);//(u2-u1)*[(cov2+cov1)/2]^-1
            tempCov = tempCov.times(dmtemp.transpose());//(u2-u1)*[(cov2+cov1)/2]^-1*(u2-u1)'
            distmean[igauss] = tempCov.A[0][0];//dist_ui

            //Compute distance between covariances
            //log[(|[cov2+cov1]/2|)/(|cov2||cov1|)^(1/2)]
            double det1 = prod(dcov1.A[0]);//|cov1| determinant of diagonal matrix UBM
            double det2 = prod(dcov2.A[0]);//|cov2| determinant of diagonal matrix Speaker
            distcov[igauss] = prod(sumCov.A[0]);//|[cov2+cov1]/2|
            distcov[igauss] = distcov[igauss]/Math.sqrt(det1*det2);//(|[cov2+cov1]/2|)/(|cov2||cov1|)^(1/2)
            distcov[igauss] = Math.log(Math.abs(distcov[igauss]));//dist COV
            if (Double.isNaN(distcov[igauss]))
            {
                distcov[igauss] = 0;
            }
        }
        //Compute Bhattacharyya
        Bha = 0.125*(sumArray(distmean))+0.5*(sumArray(distcov))-0.5*(sumArray(distprior));

        return Bha;
    }

    //Perform MAP adaptation
    private void gmmadap()
    {
        //Estimate activations
        Matrix actv = gmmprob();

        //To estimate new statistics
        newpr(actv);
        double[] rowPR = pr.getColumnPackedCopy();
        Matrix prCol = new Matrix(rowPR,pr.getColumnDimension());

        //Adaptation coefficient
        double alpha = 0;

        //Adapted parameters
        newpriors = new Matrix(1,ngauss);
        newmeanvec = new Matrix(ngauss,nfeats);
        newcovars = new Matrix(ngauss,nfeats);

        for(int i=0;i<ngauss;i++)
        {

            //Compute E = (pr(x)*x)/ni
            rowPR = prCol.A[i];
            Matrix temp = new Matrix(rowPR,1);
            Matrix E = Normfeatmat.transpose().times(temp.transpose());
            E = E.times(1/ni[i]);
            rowPR = E.getColumnPackedCopy();
            E = new Matrix(rowPR,1);
            //-------------------------------

            //Compute E2 = (pr(x)*(x^2))/ni = cov+(E^2)
            Matrix E2 = new Matrix(covars.A[i],1);
            temp = E.arrayTimes(E);
            E2.plusEquals(temp);
            //---------------------------------

            //New Prior/Weigths wi'= [alpha*ni/T+(1-alpha)*wi]*ScaleFactor
            alpha = ni[i]/(ni[i]+16);//Relevance factor r=16
            int T = Normfeatmat.getRowDimension();//Number of samples/features vectors per speaker
            newpriors.A[0][i] =  ((alpha*ni[i])/(T))+((1-alpha)*priors.A[0][i]);
            rowPR = newpriors.sum(0); //sumArray(newpriors);

            newpriors = newpriors.times(1/rowPR[0]);
            //---------------------------------

            //New mean vector ui' = alpha*Ei+(1-alpha)*ui
            temp = E.times(alpha);
            Matrix temp2 = new Matrix(centres.A[i],1);
            Matrix temp3 = temp2;//it is used later to compute the new covariance
            temp2 = temp2.times(1-alpha);
            temp = temp.plus(temp2);
            newmeanvec.A[i] = temp.A[0];
            //---------------------------------

            //New covariance covi' = alpha*Ei2+(1-alpha)*(cov+(ui^2))-(ui'^2)
            E2 = E2.times(alpha);
            temp2 = new Matrix(newmeanvec.A[i],1);
            temp2.arrayTimesEquals(temp2);//ui'^2
            temp3.arrayTimesEquals(temp3);//ui^2
            temp = new Matrix(covars.A[i],1);
            temp.plusEquals(temp3);//(cov+(ui^2))
            temp.timesEquals(1-alpha);//(1-alpha)*(cov-(ui^2))
            temp.minusEquals(temp2);//(1-alpha)*(cov-(ui^2))-(ui'^2)
            temp2 = E2.plus(temp);
            newcovars.A[i] = temp2.A[0];
            //---------------------------------
        }


    }

    //Compute the gmm pdf using precomputed paramaters for the feature standarization
    //and weights, mean vector and covariance diagonal matrix.
    private Matrix gmmprob()
    {
        Matrix act =new Matrix(samples,ngauss);
        Normfeatmat = new Matrix(samples,nfeats);
        //zscore
        zscorefeat();
            for(int igauss=0;igauss<ngauss;igauss++)
            {
                for(int isam=0;isam<samples;isam++)
                {
                    //Get inverse of diagonal covariance matrix
                    double[] temp = covars.A[igauss];
                    Matrix inv = covars.invert(temp);//cov^-1
                    //Mean vector
                    Matrix mu = new Matrix(centres.A[igauss],1);
                    //-------------------------------------------------
                    Matrix xNorm = new Matrix (Normfeatmat.A[isam],1);
                    Matrix diff = xNorm.minus(mu);//(x-u)
                    Matrix t1 = diff.times(inv);//(x-u)*(cov^-1)
                    t1 = t1.times(diff.transpose());//(x-u)*(cov^-1)*(x-u)'
                    double pow = t1.A[0][0];
                    double texp = Math.exp(-0.5*pow);//e^(-0.5*((x-u)*(cov^-1)*(x-u)'))
                    double det = prod(covars.A[igauss]);//|cov|
                    double den = Math.pow(2*Math.PI,featmat.getColumnDimension()/2)*Math.sqrt(det);//2¨PI^(D/2)*sqrt(|cov|)
                    act.A[isam][igauss] = (priors.A[0][igauss])*texp/den;//w*p(x)
            }
        }
        return act;
    }

    //Compute Pr = w*p(x)/sum(w*p(x))
    //actv Activation computed with gmmprob.
    private void newpr(Matrix actv)
    {
        //----------------------------
        pr = new Matrix(samples,ngauss);
        double[] sumPR = actv.sum(0);
        for(int m=0;m<samples;m++)
        {
            for(int n=0;n<ngauss;n++)
            {
                pr.A[m][n]=actv.A[m][n]/sumPR[m];
            }
        }
        //---------------------------
        ni = pr.sum(1);
    }

    //zscore standarization
    private void zscorefeat() {
        for (int isam = 0; isam < samples; isam++)
        {
            Matrix x = new Matrix(featmat.A[isam],1);
            Matrix xMU = new Matrix(ParamzScore.A[0], 1);
            Matrix xSIGMA = new Matrix(ParamzScore.A[1], 1);
            Matrix xNorm = x.minus(xMU);
            xNorm = xNorm.arrayRightDivide(xSIGMA);
            Normfeatmat.A[isam] = xNorm.A[0];
    }
    }

    //Compute the determinant of a diagonal Matrix
    private double prod(double[] x)
    {   double mul = x[0];
        for(int i=1;i<x.length;i++)
        {
            mul = mul*x[i];
        }
        return mul;
    }


    //vals: List to convert
    private double[] listtoarray(List vals)
    {
        double[] means = new double[vals.size()];
        for(int i=0;i<vals.size();i++)
        {
            float temp = (float) vals.get(i);
            means[i] =  temp;
        }
        return means;
    }

    //Convert list of feature vector to array
    private double[] featstoarray(List xlist,int nfeats)
    {
        //x is a list with the feature vector
        double[] x = new double[xlist.size()*nfeats];
        for(int i=0;i<xlist.size();i++)
        {
            float[] temp = (float[]) xlist.get(i);
            int ini = i*nfeats;
            int t= 0;
            for (int j = ini;j<ini+nfeats;j++)
            {
                x[j] = temp[t];
                t = t+1;
            }
        }
        return x;
    }

    //Sum all elements from array
    private double sumArray(double[] x)
    {
        double s=0;
        for(int i =0;i<x.length;i++)
        {
            s+=x[i];
        }
        return s;
    }

    //Read plain text file (rows: samples, gaussians, etc. columns: features)
    private String readtxt(File file) {

        //Read text from file
        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
            String ret = text.toString();

            return ret;
        } catch (IOException e) {
            Log.d("ERROR", e.getMessage());
            e.printStackTrace();
        }

        return "0";
    }
    private List<Float> readFileIntoArray (File filepath) {
        String file = readtxt(filepath);
        List<Float> ret = new ArrayList<>();
        String[] lines = file.split("\n");
        for (String line : lines) {
            String[] values = line.split(" ");
            for (String value : values) ret.add(Float.valueOf(value));
        }
        return ret;
    }

}

