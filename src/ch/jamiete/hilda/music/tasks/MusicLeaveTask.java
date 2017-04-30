package ch.jamiete.hilda.music.tasks;

import ch.jamiete.hilda.music.MusicServer;

public class MusicLeaveTask implements Runnable {
    private final MusicServer server;

    public MusicLeaveTask(final MusicServer server) {
        this.server = server;
    }

    @Override
    public void run() {
        server.shutdown();
    }

}
