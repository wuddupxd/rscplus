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
	long timestamp_input;
	
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
			
			timestamp_input = input.readInt();
			
			Logger.Info("ReplayServer: Starting playback");
			
			long frame_timer = System.currentTimeMillis() + (Replay.getFrameTimeSlice());
			
			for(;;) {
				if (!Replay.paused) {
					long time = System.currentTimeMillis();
				
					if (time >= frame_timer) {
						frame_timer = time + (Replay.getFrameTimeSlice());
						Replay.incrementTimestamp();
					}

					if (!doTick()) {
						client.close();
						sock.close();
						Logger.Info("ReplayServer: Playback has finished");
						return;
					}
				}
			}
		} catch (Exception e) {
			if (sock != null) {
				try {
					sock.close();
					client.close();
				} catch (Exception e2) {
				}
			}
			
			Logger.Error("ReplayServer: Failed to serve replay");
		}
	}
	
	public boolean doTick() {
		try {
			while (Replay.timestamp >= timestamp_input) {
				int length = input.readInt();
				ByteBuffer buffer = ByteBuffer.allocate(length);
				input.read(buffer.array(), 0, length);
				
				client.write(buffer);
				timestamp_input = input.readInt();
			}
			return true;
		} catch (Exception e) {
		}
		
		return false;
	}
}
