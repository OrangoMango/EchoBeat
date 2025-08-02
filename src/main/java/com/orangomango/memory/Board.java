package com.orangomango.memory;

import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Point2D;

import java.util.Random;

public class Board{
	private int w, h;
	private Tile[][] board;
	private Point2D position;

	public Board(Point2D pos, int w, int h){
		this.position = pos;
		this.w = w;
		this.h = h;
		this.board = new Tile[w][h];

		for (int x = 0; x < w; x++){
			for (int y = 0; y < h; y++){
				this.board[x][y] = new Tile(x, y);
			}
		}
	}

	public int getWidth(){
		return this.w;
	}

	public int getHeight(){
		return this.h;
	}

	public Tile getTileAt(int x, int y){
		if (x >= 0 && y >= 0 && x < this.w && y < this.h){
			return this.board[x][y];
		} else return null;
	}

	public Tile getRandomTile(){
		Random random = new Random();
		int x = random.nextInt(this.w);
		int y = random.nextInt(this.h);
		return this.board[x][y];
	}

	public void render(GraphicsContext gc){
		gc.save();
		gc.translate(this.position.getX(), this.position.getY());
		for (int x = 0; x < w; x++){
			for (int y = 0; y < h; y++){
				this.board[x][y].render(gc);
			}
		}
		gc.restore();
	}
}