package Game;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import Client.Logger;

public class ReplayServer implements Runnable {
	String playbackDirectory;
	DataInputStream input = null;
	SocketChannel client = null;
	
	public boolean isDone = false;
	
	ReplayServer(String directory) {
		playbackDirectory = directory;
	}
	
	@Override
	public void run() {
		ServerSocketChannel sock = null;
		try {
			input = new DataInputStream(new FileInputStream(new File(playbackDirectory + "/in.bin")));
			
			Logger.Info("ReplayServer: Waiting for client...");
			
			sock = ServerSocketChannel.open();
			sock.bind(new InetSocketAddress(43594));
			client = sock.accept();
			
			Logger.Info("ReplayServer: Starting playback");
			
			isDone = false;
			
			for(;;) {
				if (!Replay.paused) {
					if (!doTick()) {
						client.close();
						sock.close();
						Replay.resetFrameTimeSlice();
						isDone = true;
						Logger.Info("ReplayServer: Playback has finished");
						return;
					}
				}
				
				Thread.sleep(1);
			}
		} catch (Exception e) {
			if (sock != null) {
				try {
					sock.close();
					client.close();
					input.close();
				} catch (Exception e2) {
				}
			}
			
			Logger.Error("ReplayServer: Failed to serve replay");
		}
	}
	
	public boolean doTick() {
		try {
			// We've reached the end of the replay
			if (input.available() <= 2) {
				return false;
			}
			
			int timestamp_input = input.readInt();
			long frame_timer = System.currentTimeMillis() + Replay.getFrameTimeSlice();
			
			while (Replay.timestamp < timestamp_input) {
				long time = System.currentTimeMillis();
				if (time >= frame_timer) {
					frame_timer = time + Replay.getFrameTimeSlice();
					Replay.incrementTimestamp();
				}
				
				// Don't hammer the cpu
				Thread.sleep(1);
			}
			
			int length = input.readInt();
			ByteBuffer buffer = ByteBuffer.allocate(length);
			input.read(buffer.array());
			client.write(buffer);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
}
