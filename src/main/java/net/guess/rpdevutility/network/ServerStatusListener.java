package net.guess.rpdevutility.network;

@FunctionalInterface
public interface ServerStatusListener {
	void onStatusChanged(boolean isConnected);
}