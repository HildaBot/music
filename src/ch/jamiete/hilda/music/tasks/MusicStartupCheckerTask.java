package ch.jamiete.hilda.music.tasks;

import java.util.TimerTask;
import ch.jamiete.hilda.music.MusicServer;

public class MusicStartupCheckerTask extends TimerTask {
    private final MusicServer server;

    public MusicStartupCheckerTask(final MusicServer server) {
        this.server = server;
    }

    @Override
    public void run() {
        if (this.server.isStopping()) {
            return;
        }

        this.server.prompt();
    }

}
