package ch.jamiete.hilda.music;

import java.util.TimerTask;

public class MusicOverseer extends TimerTask {
    private final MusicServer server;

    public MusicOverseer(final MusicServer server) {
        this.server = server;
    }

    @Override
    public void run() {
        if (this.server.isStopping()) {
            return;
        }

        MusicManager.getLogger().fine("Checking whether " + this.server.getGuild().getName() + " should stay alive...");

        if (this.server.getSelf().getVoiceState().isMuted()) {
            MusicManager.getLogger().info("Server was muted; shutting down...");
            this.server.shutdown();
            return;
        }

        this.server.prompt();
    }

}
