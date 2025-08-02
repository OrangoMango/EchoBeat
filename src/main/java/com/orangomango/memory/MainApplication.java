package com.orangomango.memory;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.canvas.*;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.Font;
import javafx.animation.*;
import javafx.util.Duration;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.input.KeyCode;

import java.util.*;

public class MainApplication extends Application{
	private static final double WIDTH = 800;
	private static final double HEIGHT = 700;
	private static final int FPS = 40;

	private static final Font FONT_SMALL = Font.loadFont(MainApplication.class.getResourceAsStream("/files/font.ttf"), 25);
	private static final Font FONT_MEDIUM = Font.loadFont(MainApplication.class.getResourceAsStream("/files/font.ttf"), 32);
	private static final Font FONT_BIG = Font.loadFont(MainApplication.class.getResourceAsStream("/files/font.ttf"), 40);

	private Board board;
	private List<Integer> sequence = new ArrayList<>();
	private int seqPos = 0;
	private int amount = 1;
	private volatile boolean ready = false;
	private long elapsedTime;
	private int screenId = 0;
	private UiButton playButton, creditsButton;
	private int countdownNumber = 0;
	private int gameMode;
	private boolean hearSound = false;
	private double mouseX, mouseY;
	private Rectangle2D repButton, hintButton;
	private double mode1Offset = 0, gameoverOffset = 0;
	private Tile wrongTile;
	
	@Override
	public void start(Stage stage){
		StackPane pane = new StackPane();
		Canvas canvas = new Canvas(WIDTH, HEIGHT);
		GraphicsContext gc = canvas.getGraphicsContext2D();
		gc.setImageSmoothing(false);
		pane.getChildren().add(canvas);

		// Home screen buttons
		this.playButton = new UiButton(120, 300, () -> {
			this.screenId = 1;
			this.elapsedTime = 0;
			this.amount = 1;
			this.sequence.clear();
			this.mode1Offset = 0;
			this.gameoverOffset = 0;
			startCountdown();
			startSequence(this.amount++, 4500);
			if (this.gameMode == 1){
				startSequence(this.amount-1, 4500, true);
			}
		});

		this.creditsButton = new UiButton(120, 400, () -> {
			System.out.println("Credits");
		});

		// Board settings
		final int w = 4;
		final int h = 4;
		final Point2D pos = new Point2D(75, 165);
		this.board = new Board(pos, w, h);

		this.repButton = new Rectangle2D(640, 380, 64, 64);
		this.hintButton = new Rectangle2D(640, 465, 64, 64);

		canvas.setOnMousePressed(e -> {
			switch (this.screenId){
				case 0:
					// Home screen
					this.playButton.onClick(e.getX(), e.getY());
					this.creditsButton.onClick(e.getX(), e.getY());
					break;

				case 1:
					if (this.gameoverOffset != 0) return;

					if (this.repButton.contains(e.getX(), e.getY()) && this.gameMode != 1){ // Does not work in game mode 1
						// Repeat the entire sequence
						if (this.ready){
							startSequence(this.amount-1, 650);
						}
					} else if (this.hintButton.contains(e.getX(), e.getY())){
						// Highlight the next tile
						if (this.ready){
							new Thread(() -> {
								try {
									Tile tile = this.gameMode == 0 ? getTileById(this.sequence.get(this.seqPos)) : (this.gameMode == 2 ? getTileById(this.sequence.get(this.sequence.size()-1-this.seqPos)) : this.wrongTile);

									tile.setActivated(true);
									Thread.sleep(300);
									tile.setActivated(false);
									Thread.sleep(700);
								} catch (InterruptedException ex){
									ex.printStackTrace();
								}
							}).start();
						}
					} else {
						if (this.hearSound){
							for (int x = 0; x < w; x++){
								for (int y = 0; y < h; y++){
									Tile tile = this.board.getTileAt(x, y);
									if (tile.clicked(e.getX()-pos.getX(), e.getY()-pos.getY())){
										int id = tile.getX()+tile.getY()*this.board.getWidth();
										AssetLoader.getInstance().getAudio(id+".wav").play(); // Just play the sound
									}
								}
							}
						} else {
							if (!this.ready) return;

							for (int x = 0; x < w; x++){
								for (int y = 0; y < h; y++){
									Tile tile = this.board.getTileAt(x, y);
									if (tile.clicked(e.getX()-pos.getX(), e.getY()-pos.getY())){
										int id = tile.getX()+tile.getY()*this.board.getWidth();
										AssetLoader.getInstance().getAudio(id+".wav").play();
										if (this.gameMode == 1){
											if (tile == this.wrongTile){
												System.out.println("Correct");
												this.wrongTile = null;
											} else {
												gameover();
											}
										} else {
											if ((this.gameMode == 0 && this.sequence.get(this.seqPos) == id) || (this.gameMode == 2 && this.sequence.get(this.sequence.size()-1-this.seqPos) == id)){
												System.out.println("Correct");
												this.seqPos++;
											} else {
												gameover();
											}
										}
									}
								}
							}

							if (this.gameoverOffset == 0){
								if (this.gameMode == 1){ // Odd One Out
									if (this.wrongTile == null){
										startSequence(this.amount++, 2000);
										startSequence(this.amount-1, 2000, true);
									}
								} else {
									if (this.seqPos == this.sequence.size()){
										startSequence(this.amount++, 2000);
									}
								}
							}
						}
					}
					break;
			}
		});

		canvas.setOnMouseMoved(e -> {
			this.mouseX = e.getX();
			this.mouseY = e.getY();

			switch (this.screenId){
				case 0:
					// Home screen
					break;

				case 1:
					if (this.gameoverOffset != 0) return;

					for (int x = 0; x < w; x++){
						for (int y = 0; y < h; y++){
							Tile tile = this.board.getTileAt(x, y);
							if (tile.clicked(e.getX()-pos.getX(), e.getY()-pos.getY())){
								// Hover
								tile.setHover(true);
							} else {
								tile.setHover(false);
							}
						}
					}
					break;
			}
		});

		canvas.setFocusTraversable(true);
		canvas.setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.ESCAPE){
				this.screenId = 0;
			}

			if (this.screenId == 0){
				if (e.getCode() == KeyCode.UP){
					this.gameMode--;
					if (this.gameMode < 0){
						this.gameMode = 3+this.gameMode;
					}
				} else if (e.getCode() == KeyCode.DOWN){
					this.gameMode = (this.gameMode + 1) % 3;
				}
			} else if (this.screenId == 1){
				if (this.gameoverOffset != 0) return;

				if (e.getCode() == KeyCode.H){
					this.hearSound = true;
				}
			}
		});

		canvas.setOnKeyReleased(e -> {
			if (this.screenId == 1){
				if (this.gameoverOffset != 0) return;
				
				if (e.getCode() == KeyCode.H){
					this.hearSound = false;
				}
			}
		});

		Timeline loop = new Timeline(new KeyFrame(Duration.millis(1000.0/FPS), e -> update(gc)));
		loop.setCycleCount(Animation.INDEFINITE);
		loop.play();
		
		stage.setScene(new Scene(pane, WIDTH, HEIGHT));
		stage.setTitle("EchoBeat v1.0");
		stage.setResizable(false);
		stage.show();
	}

	private void startCountdown(){
		Thread t = new Thread(() -> {
			for (int i = 3; i >= 0; i--){
				try {
					Thread.sleep(1000);
					this.countdownNumber = i;
				} catch (InterruptedException ex){
					ex.printStackTrace();
				}
			}
		});
		t.start();
	}

	private void startMode1Screen() throws InterruptedException{
		for (int i = 0; i < WIDTH/2; i += 15){
			this.mode1Offset = i;
			Thread.sleep(30);
		}
	}

	private void gameover(){
		Thread t = new Thread(() -> {
			for (int i = 0; i < WIDTH/2; i += 15){
				try {
					this.gameoverOffset = i;
					Thread.sleep(30);
				} catch (InterruptedException ex){
					ex.printStackTrace();
				}
			}
		});
		t.start();
	}

	private void startSequence(int n, int waitTime){
		startSequence(n, waitTime, false);
	}

	private void startSequence(int n, int waitTime, boolean ooo){
		this.seqPos = 0;
		this.ready = false;

		Thread t = new Thread(() -> {
			try {
				Thread.sleep(waitTime);
				if (ooo){
					while (!this.ready){
						Thread.sleep(100);
					}
					this.ready = false;

					startMode1Screen();
					Thread.sleep(1100); // Wait before second round (ooo)
					this.mode1Offset = 0;
				}
				
				final int lastSize = this.sequence.size();
				Random random = new Random();
				int pos = lastSize > 0 ? random.nextInt(lastSize) : -1;
				
				for (int i = 0; i < n; i++){
					Tile tile = i >= lastSize ? this.board.getRandomTile() : getTileById(this.sequence.get(i));

					if (ooo && i == pos){
						do {
							tile = this.board.getRandomTile();
							this.wrongTile = tile;
						} while (tile.getX()+tile.getY()*this.board.getWidth() == this.sequence.get(i));
					}
					
					int id = tile.getX()+tile.getY()*this.board.getWidth();
					AssetLoader.getInstance().getAudio(id+".wav").play();

					if (i >= lastSize){
						this.sequence.add(id);
					}

					tile.setActivated(true);
					Thread.sleep(300);
					tile.setActivated(false);
					Thread.sleep(700);
				}
				this.ready = true;
			} catch (InterruptedException ex){
				ex.printStackTrace();
			}
		});
		t.setDaemon(true);
		t.start();
	}

	private Tile getTileById(int id){
		int x = id % this.board.getWidth();
		int y = id / this.board.getWidth();
		return this.board.getTileAt(x, y);
	}
	
	private void update(GraphicsContext gc){
		gc.clearRect(0, 0, WIDTH, HEIGHT);
		gc.setFill(Color.web("#FDFBD5"));
		gc.fillRect(0, 0, WIDTH, HEIGHT);

		if (this.ready && this.gameoverOffset == 0){
			this.elapsedTime += 1000/FPS;
		}

		if (this.screenId == 0){
			this.playButton.render(gc);
			this.creditsButton.render(gc);

			// Render game modes
			gc.setFill(Color.LIME);
			gc.fillRect(470, 150, 100, 100);
			gc.fillRect(470, 270, 100, 100);
			gc.fillRect(470, 390, 100, 100);

			gc.setFont(FONT_MEDIUM);
			gc.setTextAlign(TextAlignment.LEFT);
			gc.setFill(this.gameMode == 0 ? Color.RED : Color.BLACK);
			gc.fillText("Standard", 590, 215);
			gc.setFill(this.gameMode == 1 ? Color.RED : Color.BLACK);
			gc.fillText("Odd One\nOut", 590, 320);
			gc.setFill(this.gameMode == 2 ? Color.RED : Color.BLACK);
			gc.fillText("Reverse", 590, 455);

			gc.setFill(Color.BLACK);
			gc.setTextAlign(TextAlignment.CENTER);
			gc.setFont(FONT_BIG);
			gc.fillText(">", 420, 200+this.gameMode*120);
		} else if (this.screenId == 1){
			this.board.render(gc);

			gc.setFill(this.ready ? Color.GREEN : Color.RED);
			gc.fillOval(630, 150, 50, 50);

			long diff = this.elapsedTime / 1000;
			gc.setFill(Color.BLACK);
			gc.setTextAlign(TextAlignment.CENTER);
			gc.setFont(FONT_SMALL);
			gc.fillText(String.format("%02d:%02d\n\nLevel %d\nGame mode: %d", diff / 60, diff % 60, this.amount-1, this.gameMode+1), 655, 230);

			gc.setFill(Color.LIME);


			// Draw it disabled:
			if (this.gameMode != 1) gc.fillRect(this.repButton.getMinX(), this.repButton.getMinY(), this.repButton.getWidth(), this.repButton.getHeight());
			
			gc.fillRect(this.hintButton.getMinX(), this.hintButton.getMinY(), this.hintButton.getWidth(), this.hintButton.getHeight());


			if (this.countdownNumber > 0){
				gc.save();
				gc.setFill(Color.BLACK);
				gc.setGlobalAlpha(0.8);
				gc.fillRect(0, 0, WIDTH, HEIGHT);
				gc.restore();
				gc.setFill(this.countdownNumber == 3 ? Color.RED : (this.countdownNumber == 2 ? Color.YELLOW : Color.GREEN));
				gc.fillRect(300, 250, 200, 200);
			}

			if (this.mode1Offset > 0){
				gc.save();
				gc.setFill(Color.BLACK);
				gc.setGlobalAlpha(0.8);
				gc.fillRect(0, 0, WIDTH, HEIGHT);
				gc.restore();
				gc.setFill(Color.RED);
				gc.fillRect(WIDTH-this.mode1Offset-100, HEIGHT/2-100, 200, 200);
			}

			if (this.gameoverOffset > 0){
				gc.save();
				gc.setFill(Color.BLACK);
				gc.setGlobalAlpha(0.8);
				gc.fillRect(0, 0, WIDTH, HEIGHT);
				gc.restore();
				gc.setFill(Color.BLUE);
				gc.fillRect(WIDTH-this.gameoverOffset-100, HEIGHT/2-100, 200, 200);
			}

			if (this.hearSound){
				gc.setFill(Color.GRAY);
				gc.fillRect(this.mouseX-25, this.mouseY-25, 50, 50);
			}
		}
	}
	
	public static void main(String[] args){
		launch(args);
	}
}
