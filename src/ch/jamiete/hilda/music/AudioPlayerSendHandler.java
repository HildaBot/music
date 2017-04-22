package ch.jamiete.hilda.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import net.dv8tion.jda.core.audio.AudioSendHandler;

/**
 * @author sedmelluq
 * https://github.com/sedmelluq/lavaplayer/blob/master/demo-jda/src/main/java/com/sedmelluq/discord/lavaplayer/demo/jda/AudioPlayerSendHandler.java
 */
public class AudioPlayerSendHandler implements AudioSendHandler {
    private final AudioPlayer audioPlayer;
    private AudioFrame lastFrame;

    public AudioPlayerSendHandler(final AudioPlayer audioPlayer) {
        this.audioPlayer = audioPlayer;
    }

    @Override
    public boolean canProvide() {
        if (this.lastFrame == null) {
            this.lastFrame = this.audioPlayer.provide();
        }

        return this.lastFrame != null;
    }

    @Override
    public boolean isOpus() {
        return true;
    }

    @Override
    public byte[] provide20MsAudio() {
        if (this.lastFrame == null) {
            this.lastFrame = this.audioPlayer.provide();
        }

        final byte[] data = this.lastFrame != null ? this.lastFrame.data : null;
        this.lastFrame = null;

        return data;
    }
}
