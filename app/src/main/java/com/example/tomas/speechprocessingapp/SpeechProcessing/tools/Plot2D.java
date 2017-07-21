package com.example.tomas.speechprocessingapp.SpeechProcessing.tools;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

//This class was implemented by Ankit Srivastava. Please consider acknowledging the author if it
// ends up being useful to you.

public class Plot2D extends View {

	private Paint paint;
	private float[] xvalues = { 0.0f };
	private float[] yvalues = { 0.0f };
	private float maxx = 0.0f, maxy = 0.0f, minx = 0.0f, miny = 0.0f,
			locxAxis = 0.0f, locyAxis = 0.0f;
	private int vectorLength;
	private int axes = 1;

	public Plot2D(Context context, AttributeSet attrs) {
		super(context, attrs);
		paint = new Paint();
	}
    public Plot2D(Context context, float[] xvalues, float[] yvalues, int axes) {
        super(context);
        this.xvalues = xvalues;
        this.yvalues = yvalues;
        this.axes = axes;
        vectorLength = xvalues.length;
        paint = new Paint();

        getAxes(xvalues, yvalues);
        invalidate();

    }

	public void plotData(float[] xvalues, float[] yvalues, int axes) {
		this.xvalues = xvalues;
		this.yvalues = yvalues;
		this.axes = axes;
		vectorLength = xvalues.length;
		// paint = new Paint();

		getAxes(xvalues, yvalues);
		invalidate();
	}

	public void plotData(float[] yvalues, int axes) {
		if (yvalues != null) {
			vectorLength = yvalues.length;
			this.xvalues = new float[vectorLength];
			for (int i = 0; i < vectorLength; i++) {
				xvalues[i] = i;
			}
			this.yvalues = yvalues;
			this.axes = axes;
			// paint = new Paint();

			getAxes(xvalues, yvalues);
			invalidate();
		}
	}

	public void plotDataLog10(float[] yvalues, int axes) {
		if (yvalues != null) {
			float[] log10YValues = new float[yvalues.length / 2];
			for (int i = 0; i < yvalues.length / 2; i++) {
				log10YValues[i] = (float) (10.0f * Math
						.log10(yvalues[i] + 1e-6));
			}
			plotData(log10YValues, axes);
		}
	}

	public void plotDataLog10Log10(float[] yvalues, int axes) {
		if (yvalues != null) {
			float[] log10YValues = new float[yvalues.length / 2];
			for (int i = 0; i < yvalues.length / 2; i++) {
				log10YValues[i] = (float) (10.0f * Math
						.log10(yvalues[i] + 1e-6));
			}
			float[] log10XValues = new float[yvalues.length / 2];
			for (int i = 0; i < yvalues.length / 2; i++) {
				log10XValues[i] = (float) (10.0f * Math
						.log10(i + 1));
			}
			plotData(log10XValues, log10YValues, axes);
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {

		float canvasHeight = getHeight();
		float canvasWidth = getWidth();
		int[] xvaluesInPixels = toPixel(canvasWidth, minx, maxx, xvalues);
		int[] yvaluesInPixels = toPixel(canvasHeight, 0, maxy, yvalues);
		int locxAxisInPixels = toPixelInt(canvasHeight, miny, maxy, locxAxis);
		int locyAxisInPixels = toPixelInt(canvasWidth, minx, maxx, locyAxis);

		paint.setStrokeWidth(2);
		paint.setColor(Color.parseColor("#F44336"));
		for (int i = 0; i < vectorLength - 1; i++) {
			paint.setColor(Color.RED);
			canvas.drawLine(xvaluesInPixels[i], canvasHeight
					- yvaluesInPixels[i], xvaluesInPixels[i + 1], canvasHeight
					- yvaluesInPixels[i + 1], paint);
		}

		paint.setColor(Color.BLACK);
		canvas.drawLine(0, canvasHeight - locxAxisInPixels, canvasWidth,
				canvasHeight - locxAxisInPixels, paint);
		canvas.drawLine(locyAxisInPixels, 0, locyAxisInPixels, canvasHeight,
				paint);

		// Automatic axes markings, modify n to control the number of axes
		// labels
		if (axes != 0) {
			float temp = 0.0f;
			int n = 3;
			paint.setTextAlign(Paint.Align.CENTER);
			paint.setTextSize(20.0f);
			for (int i = 1; i <= n; i++) {
				temp = Math.round(10 * (minx + (i - 1) * (maxx - minx) / n)) / 10;
				canvas.drawText("" + temp,
						(float) toPixelInt(canvasWidth, minx, maxx, temp),
						canvasHeight - locxAxisInPixels + 20, paint);
				temp = Math.round(10 * (miny + (i - 1) * (maxy - miny) / n)) / 10;
				canvas.drawText("" + temp, locyAxisInPixels + 20, canvasHeight
						- (float) toPixelInt(canvasHeight, miny, maxy, temp),
						paint);
			}
			canvas.drawText("" + maxx,
					(float) toPixelInt(canvasWidth, minx, maxx, maxx),
					canvasHeight - locxAxisInPixels + 20, paint);
			canvas.drawText("" + maxy, locyAxisInPixels + 20, canvasHeight
					- (float) toPixelInt(canvasHeight, miny, maxy, maxy), paint);
			// canvas.drawText(xAxis,
			// canvasWidth/2,canvasHeight-locxAxisInPixels+45, paint);
			// canvas.drawText(yAxis, locyAxisInPixels-40,canvasHeight/2,
			// paint);
		}

	}

	private int[] toPixel(float pixels, float min, float max, float[] value) {
		double[] p = new double[value.length];
		int[] pint = new int[value.length];

		for (int i = 0; i < value.length; i++) {
			p[i] = .1 * pixels + ((value[i] - min) / (max - min)) * .8 * pixels;
			pint[i] = (int) p[i];
		}

		return (pint);
	}

	private void getAxes(float[] xvalues, float[] yvalues) {

		minx = getMin(xvalues);
		miny = getMin(yvalues);
		maxx = getMax(xvalues);
		maxy = getMax(yvalues);

		if (minx >= 0)
			locyAxis = minx;
		else if (minx < 0 && maxx >= 0)
			locyAxis = 0;
		else
			locyAxis = maxx;

		if (miny >= 0)
			locxAxis = miny;
		else if (miny < 0 && maxy >= 0)
			locxAxis = 0;
		else
			locxAxis = maxy;

	}

	private int toPixelInt(float pixels, float min, float max, float value) {

		double p;
		int pint;
		p = .1 * pixels + ((value - min) / (max - min)) * .8 * pixels;
		pint = (int) p;
		return (pint);
	}

	private float getMax(float[] v) {
		float largest = v[0];
		for (int i = 0; i < v.length; i++)
			if (v[i] > largest)
				largest = v[i];
		return largest;
	}

	private float getMin(float[] v) {
		float smallest = v[0];
		for (int i = 0; i < v.length; i++)
			if (v[i] < smallest)
				smallest = v[i];
		return smallest;
	}

}
