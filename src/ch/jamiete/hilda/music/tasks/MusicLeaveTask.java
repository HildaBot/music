package ch.jamiete.hilda.music.tasks;

import java.util.concurrent.TimeUnit;
import ch.jamiete.hilda.Hilda;
import ch.jamiete.hilda.music.MusicServer;

public class MusicLeaveTask implements Runnable {
    private Hilda hilda;
    private MusicServer server;

    public MusicLeaveTask(Hilda hilda, MusicServer server) {
        this.hilda = hilda;
        this.server = server;
    }

    @Override
    public void run() {
        server.shutdown();

        if (!server.isStopping()) {
            hilda.getExecutor().schedule(new MusicLeaveTask(hilda, server), 30, TimeUnit.MINUTES);
        }
    }

}
