package com.orangomango.memory;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.geometry.Rectangle2D;

public class UiButton{
	private double x, y;
	private Runnable onClick = null;

	private static final double WIDTH = 200;
	private static final double HEIGHT = 75;

	public UiButton(double x, double y, Runnable r){
		this.x = x;
		this.y = y;
		this.onClick = r;
	}

	public void onClick(double ex, double ey){
		Rectangle2D rect = new Rectangle2D(this.x, this.y, WIDTH, HEIGHT);
		if (rect.contains(ex, ey)){
			this.onClick.run();
		}
	}

	public void render(GraphicsContext gc){
		gc.setFill(Color.RED);
		gc.fillRect(this.x, this.y, WIDTH, HEIGHT);
	}
}