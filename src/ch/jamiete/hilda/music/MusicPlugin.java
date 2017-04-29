package ch.jamiete.hilda.music;

import ch.jamiete.hilda.Hilda;
import ch.jamiete.hilda.music.commands.MusicBaseCommand;
import ch.jamiete.hilda.plugins.HildaPlugin;

public class MusicPlugin extends HildaPlugin {

    private MusicManager music;

    public MusicPlugin(Hilda hilda) {
        super(hilda);
    }

    @Override
    public void onEnable() {
        this.music = new MusicManager(getHilda());
        getHilda().getCommandManager().registerChannelCommand(new MusicBaseCommand(getHilda(), this.music));
    }

    public MusicManager getMusicManager() {
        return music;
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onLoad() {
    }

}
