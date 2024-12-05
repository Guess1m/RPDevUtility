package com.Guess.ReportsPlusServer.util.network;

@FunctionalInterface
public interface ServerStatusListener {
	void onStatusChanged(boolean isConnected);
}