package ch.jamiete.hilda.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

public class QueueItem {
    private final AudioTrack track;
    private final String user;

    public QueueItem(final AudioTrack track, final String user) {
        this.track = track;
        this.user = user;
    }

    /**
     * Gets the track held in the item.
     * @return The track.
     */
    public AudioTrack getTrack() {
        return this.track;
    }

    /**
     * Gets the ID of the user who queued the item.
     * @return The user ID.
     */
    public String getUserId() {
        return this.user;
    }

    @Override
    public String toString() {
        return "QueueItem{track=" + this.track.getIdentifier() + ", user=" + this.user + "}";
    }

}
