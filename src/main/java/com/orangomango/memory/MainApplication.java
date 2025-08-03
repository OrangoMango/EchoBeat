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
import javafx.scene.media.MediaPlayer;

import java.util.*;

public class MainApplication extends Application{
	private static final double WIDTH = 800;
	private static final double HEIGHT = 700;
	private static final int FPS = 40;

	private static final Font FONT_SMALLSMALL = Font.loadFont(MainApplication.class.getResourceAsStream("/files/font.ttf"), 17);
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
	private int repCount = 3, hintCount = 3;
	private int repExtraSize = 0, hintExtraSize = 0;
	private long lastCreditCheck;
	private double hsb;
	
	@Override
	public void start(Stage stage){
		StackPane pane = new StackPane();
		Canvas canvas = new Canvas(WIDTH, HEIGHT);
		GraphicsContext gc = canvas.getGraphicsContext2D();
		gc.setImageSmoothing(false);
		pane.getChildren().add(canvas);

		MediaPlayer player = new MediaPlayer(AssetLoader.getInstance().getMusic("bgmusic.wav"));
		player.setVolume(0.1);
		player.setCycleCount(MediaPlayer.INDEFINITE);
		player.play();

		// Home screen buttons
		this.playButton = new UiButton(120, 300, () -> {
			this.screenId = 1;
			this.elapsedTime = 0;
			this.amount = 1;
			this.sequence.clear();
			this.mode1Offset = 0;
			this.gameoverOffset = 0;
			this.repCount = 3;
			this.hintCount = 3;
			startCountdown();
			startSequence(this.amount++, 5000);
			if (this.gameMode == 1){
				startSequence(this.amount-1, 4500, true);
			}
		}, "button_play.png");

		this.creditsButton = new UiButton(120, 420, () -> {
			this.screenId = 2;
		}, "button_credits.png");

		// Board settings
		final int w = 4;
		final int h = 4;
		final Point2D pos = new Point2D(75, 195);
		this.board = new Board(pos, w, h);

		this.repButton = new Rectangle2D(640, 410, 64, 64);
		this.hintButton = new Rectangle2D(640, 495, 64, 64);

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
						if (this.ready && this.repCount > 0){
							this.repCount--;
							AssetLoader.getInstance().getAudio("click.wav").play();
							startSequence(this.amount-1, 650);
						}
					} else if (this.hintButton.contains(e.getX(), e.getY())){
						// Highlight the next tile
						if (this.ready && this.hintCount > 0){
							this.hintCount--;
							AssetLoader.getInstance().getAudio("click.wav").play();
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
												this.wrongTile = null;
											} else {
												gameover();
											}
										} else {
											if ((this.gameMode == 0 && this.sequence.get(this.seqPos) == id) || (this.gameMode == 2 && this.sequence.get(this.sequence.size()-1-this.seqPos) == id)){
												this.seqPos++;
											} else {
												gameover();
											}
										}
									}
								}
							}

							schedule(() -> {
								if (this.gameoverOffset == 0){
									if (this.gameMode == 1){ // Odd One Out
										if (this.wrongTile == null){
											AssetLoader.getInstance().getAudio("levelup.wav").play();
											startSequence(this.amount++, 2000);
											startSequence(this.amount-1, 2000, true);
										}
									} else {
										if (this.seqPos == this.sequence.size()){
											AssetLoader.getInstance().getAudio("levelup.wav").play();
											startSequence(this.amount++, 2000);
										}
									}
								}
							}, 500);
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
					this.playButton.hover(e.getX(), e.getY());
					this.creditsButton.hover(e.getX(), e.getY());
					break;

				case 1:
					if (this.gameoverOffset != 0) return;

					if (this.repButton.contains(e.getX(), e.getY())){
						this.repExtraSize = 1;
					} else {
						this.repExtraSize = 0;
					}

					if (this.hintButton.contains(e.getX(), e.getY())){
						this.hintExtraSize = 1;
					} else {
						this.hintExtraSize = 0;
					}

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
			if (e.getCode() == KeyCode.ESCAPE && (this.screenId != 1 || this.ready)){
				this.screenId = 0;
			}

			if (this.screenId == 0){
				if (e.getCode() == KeyCode.UP){
					this.gameMode--;
					if (this.gameMode < 0){
						this.gameMode = 3+this.gameMode;
					}
					AssetLoader.getInstance().getAudio("gamemode.wav").play();
				} else if (e.getCode() == KeyCode.DOWN){
					this.gameMode = (this.gameMode + 1) % 3;
					AssetLoader.getInstance().getAudio("gamemode.wav").play();
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
		stage.getIcons().add(AssetLoader.getInstance().getImage("icon.png"));
		stage.setResizable(false);
		stage.show();
	}

	private void schedule(Runnable r, int delay){
		Thread t = new Thread(() -> {
			try {
				Thread.sleep(delay);
				r.run();
			} catch (InterruptedException ex){
				ex.printStackTrace();
			}
		});
		t.start();
	}

	private void startCountdown(){
		Thread t = new Thread(() -> {
			for (int i = 3; i >= 0; i--){
				try {
					Thread.sleep(1000);
					AssetLoader.getInstance().getAudio(i == 0 ? "go.mp3" : "countdown.wav").play();
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
			AssetLoader.getInstance().getAudio("gameover.wav").play();
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

					if (this.amount <= 3){
						AssetLoader.getInstance().getAudio("mode1.wav").play();
						startMode1Screen();
						Thread.sleep(1600); // Wait before second round (ooo)
						this.mode1Offset = 0;
					} else {
						Thread.sleep(700);
					}
					Thread.sleep(1000);
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
		gc.drawImage(AssetLoader.getInstance().getImage("background.png"), 0, 0, WIDTH, HEIGHT);

		gc.drawImage(AssetLoader.getInstance().getImage("logo.png"), 100, 15, 600, 150);

		if (this.ready && this.gameoverOffset == 0){
			this.elapsedTime += 1000/FPS;
		}

		if (this.screenId == 0){
			this.playButton.render(gc);
			this.creditsButton.render(gc);

			// Render game modes
			gc.drawImage(AssetLoader.getInstance().getImage("gamemode.png"), 1, 1, 32, 32, 470, 215, 100, 100);
			gc.drawImage(AssetLoader.getInstance().getImage("gamemode.png"), 35, 1, 32, 32, 470, 335, 100, 100);
			gc.drawImage(AssetLoader.getInstance().getImage("gamemode.png"), 69, 1, 32, 32, 470, 455, 100, 100);

			gc.setFont(FONT_MEDIUM);
			gc.setTextAlign(TextAlignment.LEFT);
			gc.setFill(this.gameMode == 0 ? Color.RED : Color.web("#FDA619"));
			gc.fillText("Standard", 590, 275);
			gc.setFill(this.gameMode == 1 ? Color.RED : Color.web("#FDA619"));
			gc.fillText("Odd One\nOut", 590, 375);
			gc.setFill(this.gameMode == 2 ? Color.RED : Color.web("#FDA619"));
			gc.fillText("Reverse", 590, 520);

			gc.setFill(Color.web("#FDA619"));
			gc.setTextAlign(TextAlignment.CENTER);
			gc.setFont(FONT_BIG);
			gc.fillText("->", 420, 265+this.gameMode*120);
		} else if (this.screenId == 1){
			this.board.render(gc);

			gc.drawImage(AssetLoader.getInstance().getImage("indicator.png"), 1+(this.ready ? 1 : 0)*34, 1, 32, 32, 630, 180, 50, 50);

			long diff = this.elapsedTime / 1000;
			gc.setFill(Color.WHITE);
			gc.setTextAlign(TextAlignment.CENTER);
			gc.setFont(FONT_SMALL);
			gc.fillText(String.format("%02d:%02d\n\nLevel %d\nGame mode: %d", diff / 60, diff % 60, this.amount-1, this.gameMode+1), 655, 260);
			
			gc.setFont(FONT_SMALLSMALL);
			gc.setTextAlign(TextAlignment.LEFT);
			gc.fillText("Hold [H] to listen", 600, 650);

			gc.drawImage(AssetLoader.getInstance().getImage("repeatbutton.png"), 1+34*(this.gameMode == 1 ? 3 : 3-this.repCount), 1, 32, 32, this.repButton.getMinX()-10*this.repExtraSize, this.repButton.getMinY()-10*this.repExtraSize, this.repButton.getWidth()+20*this.repExtraSize, this.repButton.getHeight()+20*this.repExtraSize);
			gc.drawImage(AssetLoader.getInstance().getImage("hintbutton.png"), 1+34*(3-this.hintCount), 1, 32, 32, this.hintButton.getMinX()-10*this.hintExtraSize, this.hintButton.getMinY()-10*this.hintExtraSize, this.hintButton.getWidth()+20*this.hintExtraSize, this.hintButton.getHeight()+20*this.hintExtraSize);

			if (this.countdownNumber > 0){
				gc.save();
				gc.setFill(Color.BLACK);
				gc.setGlobalAlpha(0.8);
				gc.fillRect(0, 0, WIDTH, HEIGHT);
				gc.restore();
				gc.drawImage(AssetLoader.getInstance().getImage("countdown.png"), 1+34*(3-this.countdownNumber), 1, 32, 32, 300, 250, 200, 200);
			}

			if (this.mode1Offset > 0){
				gc.save();
				gc.setFill(Color.BLACK);
				gc.setGlobalAlpha(0.8);
				gc.fillRect(0, 0, WIDTH, HEIGHT);
				gc.restore();
				gc.drawImage(AssetLoader.getInstance().getImage("mode1.png"), WIDTH-this.mode1Offset-100, HEIGHT/2-180, 200, 200);
				gc.setFill(Color.BLUE);
				gc.setTextAlign(TextAlignment.CENTER);
				gc.setFont(FONT_BIG);
				gc.fillText("Now spot the wrong beat!", WIDTH/2, HEIGHT/2+120);
			}

			if (this.gameoverOffset > 0){
				gc.save();
				gc.setFill(Color.BLACK);
				gc.setGlobalAlpha(0.9);
				gc.fillRect(0, 0, WIDTH, HEIGHT);
				gc.restore();
				gc.drawImage(AssetLoader.getInstance().getImage("gameover.png"), WIDTH-this.gameoverOffset-100, HEIGHT/2-220, 200, 200);
				gc.setFill(Color.RED);
				gc.setTextAlign(TextAlignment.CENTER);
				gc.setFont(FONT_BIG);
				gc.fillText(String.format("You reached level %d in %02d:%02d.\nYou can do better!  [ESC]", this.amount-1, diff / 60, diff % 60), WIDTH/2, HEIGHT/2+150);
			}

			if (this.hearSound){
				gc.drawImage(AssetLoader.getInstance().getImage("volume.png"), this.mouseX-25, this.mouseY-25, 50, 50);
			}
		} else if (this.screenId == 2){
			gc.setFill(Color.hsb(this.hsb, 1, 1));
			long now = System.currentTimeMillis();
			if (now-this.lastCreditCheck > 50){
				this.lastCreditCheck = now;
				this.hsb = (this.hsb + 5) % 360;
			}
			gc.setFont(FONT_BIG);
			gc.setTextAlign(TextAlignment.CENTER);
			gc.fillText("GMTK Game Jam 2025\nCode and Images by OrangoMango\n\nhttps://orangomango.github.io\n\n---- [ESC] ----\nEchoBeat v1.0\n> Made with Java and JavaFX,\nno game engine :)", WIDTH/2, HEIGHT/2-100);
		}
	}
	
	public static void main(String[] args){
		launch(args);
	}
}