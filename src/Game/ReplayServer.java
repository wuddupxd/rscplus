package Game;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import Client.Logger;

public class ReplayServer implements Runnable {
	String playbackDirectory;
	DataInputStream input = null;
	
	ReplayServer(String directory) {
		playbackDirectory = directory;
	}
	
	@Override
	public void run() {
		ServerSocket sock = null;
		try {
			input = new DataInputStream(new FileInputStream(new File(playbackDirectory + "/in.bin")));
			
			Logger.Info("ReplayServer: Waiting for client...");
			
			sock = new ServerSocket(43594);
			Socket client = sock.accept();
			
			Logger.Info("ReplayServer: Starting playback");
			
			long input_timestamp_adjust = input.readLong();
			long server_timestamp_adjust = System.currentTimeMillis();
			long timestamp = 0;
			for (;;) {
				long time = System.currentTimeMillis() - server_timestamp_adjust;
				if (time >= timestamp) {
					int length = input.readInt();
					byte[] data = new byte[length];
					input.read(data, 0, length);
					
					client.getOutputStream().write(data, 0, length);
					timestamp = input.readLong() - input_timestamp_adjust;
				}
				Thread.sleep(1);
			}
		} catch (Exception e) {
			if (sock != null) {
				try { sock.close(); } catch (Exception e2) {}
			}
			
			Logger.Error("ReplayServer: Failed to serve replay");
		}
	}
}
