package com.orangomango.memory;

import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Rectangle2D;

public class UiButton{
	private double x, y;
	private Runnable onClick = null;
	private String imageName;
	private double extraSize;
	private long lastCheck;
	private double offsetY;
	private int direction = 1;

	private static final double WIDTH = 200;
	private static final double HEIGHT = 75;

	public UiButton(double x, double y, Runnable r, String imageName){
		this.x = x;
		this.y = y;
		this.onClick = r;
		this.imageName = imageName;
	}

	public void onClick(double ex, double ey){
		Rectangle2D rect = new Rectangle2D(this.x, this.y+this.offsetY, WIDTH, HEIGHT);
		if (rect.contains(ex, ey)){
			AssetLoader.getInstance().getAudio("clickmenu.wav").play();
			this.onClick.run();
		}
	}

	public void hover(double ex, double ey){
		Rectangle2D rect = new Rectangle2D(this.x, this.y+this.offsetY, WIDTH, HEIGHT);
		if (rect.contains(ex, ey)){
			this.extraSize = 1;
		} else {
			this.extraSize = 0;
		}
	}

	public void render(GraphicsContext gc){
		gc.drawImage(AssetLoader.getInstance().getImage(this.imageName), this.x-15*this.extraSize, this.y-15*this.extraSize+this.offsetY, WIDTH+30*this.extraSize, HEIGHT+30*this.extraSize);

		final long now = System.currentTimeMillis();
		if (now-this.lastCheck > 80){
			this.lastCheck = now;
			this.offsetY = this.offsetY + 2 * this.direction;
			if (this.offsetY >= 20 || this.offsetY <= 0){
				this.direction *= -1;
			}
		}
	}
}