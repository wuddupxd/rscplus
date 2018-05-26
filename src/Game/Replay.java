/**
 *	rscplus
 *
 *	This file is part of rscplus.
 *
 *	rscplus is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	rscplus is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with rscplus.  If not, see <http://www.gnu.org/licenses/>.
 *
 *	Authors: see <https://github.com/OrN/rscplus>
 */

package Game;

import java.awt.event.KeyEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import Client.Logger;
import Client.Settings;
import Client.Util;

public class Replay {
	static DataOutputStream output = null;
	static DataOutputStream input = null;
	static DataOutputStream keys = null;
	static DataOutputStream keyboard = null;
	static DataOutputStream mouse = null;
	
	static DataInputStream play_keys = null;
	static DataInputStream play_keyboard = null;
	
	public static final byte KEYBOARD_TYPED = 0;
	public static final byte KEYBOARD_PRESSED = 1;
	public static final byte KEYBOARD_RELEASED = 2;
	
	public static boolean isPlaying = false;
	public static boolean isRecording = false;
	
	public static long timestamp;
	public static long timestamp_adjust;
	public static long timestamp_kb_input;
	
	public static void generateTimestamp() {
		timestamp = System.currentTimeMillis() - timestamp_adjust;
	}
	
	public static void initializeReplayPlayback(String replayDirectory) {
		try {
			play_keys = new DataInputStream(new FileInputStream(new File(replayDirectory + "/keys.bin")));
			play_keyboard = new DataInputStream(new FileInputStream(new File(replayDirectory + "/keyboard.bin")));
			
			timestamp = 0;
			timestamp_adjust = System.currentTimeMillis();
			timestamp_kb_input = play_keyboard.readLong();
		} catch (Exception e) {
			play_keys = null;
			play_keyboard = null;
			Logger.Error("Failed to initialize replay playback");
			return;
		}
		Game.getInstance().getJConfig().changeWorld(6);
		new Thread(new ReplayServer(replayDirectory)).start();
		isPlaying = true;
		Logger.Info("Replay playback started");
	}
	
	public static void closeReplayPlayback() {
		if (play_keys == null)
			return;
		
		try {
			play_keys.close();
			play_keyboard.close();
			
			play_keys = null;
			play_keyboard = null;
		} catch (Exception e) {
			play_keys = null;
			play_keyboard = null;
		}
		
		Game.getInstance().getJConfig().changeWorld(Settings.WORLD);
		isPlaying = false;
		Logger.Info("Replay playback stopped");
	}
	
	public static void initializeReplayRecording() {
		// No username specified, exit
		if (Client.username_login.length() == 0)
			return;
		
		String timeStamp = new SimpleDateFormat("MM-dd-yyyy HH.mm.ss").format(new Date());
		
		String recordingDirectory = Settings.Dir.REPLAY + "/" + Client.username_login;
		Util.makeDirectory(recordingDirectory);
		recordingDirectory = recordingDirectory + "/" + timeStamp;
		Util.makeDirectory(recordingDirectory);
		
		try {
			output = new DataOutputStream(new FileOutputStream(new File(recordingDirectory + "/out.bin")));
			input = new DataOutputStream(new FileOutputStream(new File(recordingDirectory + "/in.bin")));
			keys = new DataOutputStream(new FileOutputStream(new File(recordingDirectory + "/keys.bin")));
			keyboard = new DataOutputStream(new FileOutputStream(new File(recordingDirectory + "/keyboard.bin")));
			mouse = new DataOutputStream(new FileOutputStream(new File(recordingDirectory + "/mouse.bin")));
			timestamp = 0;
			timestamp_adjust = System.currentTimeMillis();
			
			Logger.Info("Replay recording started");
		} catch (Exception e) {
			output = null;
			input = null;
			keys = null;
			keyboard = null;
			mouse = null;
			Logger.Error("Unable to create replay files");
			return;
		}
		
		isRecording = true;
	}
	
	public static void closeReplayRecording() {
		if (output == null)
			return;
		
		try {
			output.close();
			input.close();
			keys.close();
			keyboard.close();
			mouse.close();
			
			output = null;
			input = null;
			keys = null;
			keyboard = null;
			mouse = null;
			
			Logger.Info("Replay recording stopped");
		} catch (Exception e) {
			output = null;
			input = null;
			keys = null;
			keyboard = null;
			mouse = null;
			Logger.Error("Unable to close replay files");
			return;
		}
		
		isRecording = false;
	}
	
	public static void playKeyboardInput() {
		try {
			while (timestamp >= timestamp_kb_input) {
				byte event = play_keyboard.readByte();
				char keychar = play_keyboard.readChar();
				int keycode = play_keyboard.readInt();
				int modifier = play_keyboard.readInt();
				KeyEvent keyEvent;
				switch (event) {
				case KEYBOARD_PRESSED:
					keyEvent = new KeyEvent(Game.getInstance().getApplet(), KeyEvent.KEY_PRESSED, timestamp, modifier, keycode, keychar);
					Client.handler_keyboard.keyPressed(keyEvent);
					break;
				case KEYBOARD_RELEASED:
					keyEvent = new KeyEvent(Game.getInstance().getApplet(), KeyEvent.KEY_RELEASED, timestamp, modifier, keycode, keychar);
					Client.handler_keyboard.keyReleased(keyEvent);
					break;
				case KEYBOARD_TYPED:
					keyEvent = new KeyEvent(Game.getInstance().getApplet(), KeyEvent.KEY_TYPED, timestamp, modifier, keycode, keychar);
					Client.handler_keyboard.keyTyped(keyEvent);
					break;
				}
				timestamp_kb_input = play_keyboard.readLong();
			}
		} catch (Exception e) {
		}
	}
	
	public static void dumpKeyboardInput(int keycode, byte event, char keychar, int modifier) {
		try {
			keyboard.writeLong(timestamp);
			keyboard.writeByte(event);
			keyboard.writeChar(keychar);
			keyboard.writeInt(keycode);
			keyboard.writeInt(modifier);
		} catch (Exception e) {
		}
	}
	
	public static void dumpMouseInput() {
	}
	
	public static void dumpRawInputStream(byte[] b, int n, int n2, int n5, int bytesread) {
		if (input == null)
			return;
		
		int off = n2 + n5;
		int len = -n5 + n;
		
		try {
			input.writeLong(timestamp);
			input.writeInt(bytesread);
			input.write(b, off, bytesread);
		} catch (Exception e) {
		}
	}
	
	public static void dumpRawOutputStream(byte[] b, int off, int len) {
		if (output == null)
			return;
		
		try {
			output.writeLong(timestamp);
			output.writeInt(len);
			output.write(b, off, len);
		} catch (Exception e) {
		}
	}
	
	public static int hookXTEAKey(int key) {
		if (play_keys != null) {
			try {
				return play_keys.readInt();
			} catch (Exception e) {
				return key;
			}
		}
		
		if (keys == null)
			return key;
		
		try {
			keys.writeInt(key); // data length
		} catch (Exception e) {
		}
		
		return key;
	}
}
