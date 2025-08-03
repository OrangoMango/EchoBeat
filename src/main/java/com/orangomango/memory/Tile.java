package com.orangomango.memory;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.geometry.Rectangle2D;

public class Tile{
	public static final double SIZE = 100;
	public static final double GAP = 20;

	private int x, y;
	private Color color;
	private boolean activated;
	private boolean hover = false;

	public Tile(int x, int y){
		this.x = x;
		this.y = y;
		this.color = Color.color(Math.random(), Math.random(), Math.random());
	}

	public int getX(){
		return this.x;
	}

	public int getY(){
		return this.y;
	}

	public boolean clicked(double px, double py){
		Rectangle2D rect = new Rectangle2D(this.x*(SIZE+GAP), this.y*(SIZE+GAP), SIZE, SIZE);
		return rect.contains(px, py);
	}

	public void setActivated(boolean value){
		this.activated = value;
	}

	public void setHover(boolean value){
		this.hover = value;
	}

	public void render(GraphicsContext gc){
		gc.setFill(this.color);
		gc.fillRect(this.x*(SIZE+GAP)+10, this.y*(SIZE+GAP)+10, SIZE-20, SIZE-20);
		gc.drawImage(AssetLoader.getInstance().getImage("tile.png"), 1+34*(this.hover ? 1 : 0), 1, 32, 32, this.x*(SIZE+GAP), this.y*(SIZE+GAP), SIZE, SIZE);
		if (this.activated){
			gc.save();
			gc.setFill(Color.WHITE);
			gc.setGlobalAlpha(0.8);
			gc.fillRect(this.x*(SIZE+GAP)+5, this.y*(SIZE+GAP)+5, SIZE-10, SIZE-10);
			gc.restore();
		}
	}
}