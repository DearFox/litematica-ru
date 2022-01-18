package fi.dy.masa.litematica.scheduler.tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import com.google.common.collect.ArrayListMultimap;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.WorldUtils;

public abstract class TaskProcessChunkBase extends TaskBase
{
    protected final ArrayListMultimap<ChunkPos, IntBoundingBox> boxesInChunks = ArrayListMultimap.create();
    protected final ArrayList<ChunkPos> pendingChunks = new ArrayList<>();
    protected final ClientWorld clientWorld;
    protected final WorldSchematic schematicWorld;
    protected final World world;
    protected final boolean isClientWorld;
    protected PositionUtils.ChunkPosComparator comparator = new PositionUtils.ChunkPosComparator();

    protected TaskProcessChunkBase(String nameOnHud)
    {
        this.clientWorld = this.mc.world;
        this.world = WorldUtils.getBestWorld(this.mc);
        this.schematicWorld = SchematicWorldHandler.getSchematicWorld();
        this.isClientWorld = (this.world == this.mc.world);
        this.name = StringUtils.translate(nameOnHud);
        this.comparator.setClosestFirst(true);

        InfoHud.getInstance().addInfoHudRenderer(this, true);
    }

    @Override
    public boolean canExecute()
    {
        return super.canExecute() && this.pendingChunks.isEmpty() == false;
    }

    @Override
    public boolean execute()
    {
        return this.executeForAllPendingChunks();
    }

    @Override
    public void stop()
    {
        // Multiplayer, just a client world
        if (this.isClientWorld)
        {
            this.onStop();
        }
        // Single player, operating in the integrated server world
        else
        {
            this.mc.execute(this::onStop);
        }
    }

    protected void onStop()
    {
        this.notifyListener();
    }

    protected abstract boolean canProcessChunk(ChunkPos pos);

    protected boolean processChunk(ChunkPos pos)
    {
        return true;
    }

    protected boolean executeForAllPendingChunks()
    {
        Iterator<ChunkPos> iterator = this.pendingChunks.iterator();
        int processed = 0;

        while (iterator.hasNext())
        {
            ChunkPos pos = iterator.next();

            if (this.canProcessChunk(pos) && this.processChunk(pos))
            {
                iterator.remove();
                ++processed;
            }
        }

        if (processed > 0)
        {
            this.updateInfoHudLinesPendingChunks(this.pendingChunks);
        }

        this.finished = this.pendingChunks.isEmpty();

        return this.finished;
    }

    protected void addPerChunkBoxes(Collection<Box> allBoxes)
    {
        this.boxesInChunks.clear();
        this.pendingChunks.clear();

        PositionUtils.getPerChunkBoxes(allBoxes, this.boxesInChunks::put);
        this.pendingChunks.addAll(this.boxesInChunks.keySet());

        this.sortChunkList();
    }

    protected List<IntBoundingBox> getBoxesInChunk(ChunkPos pos)
    {
        return this.boxesInChunks.get(pos);
    }

    protected void sortChunkList()
    {
        if (this.pendingChunks.size() > 0)
        {
            if (this.mc.player != null)
            {
                this.comparator.setReferencePosition(this.mc.player.getBlockPos());
                this.pendingChunks.sort(this.comparator);
            }

            this.updateInfoHudLines();
            this.onChunkListSorted();
        }
    }

    protected void onChunkListSorted()
    {
    }

    protected void updateInfoHudLines()
    {
        this.updateInfoHudLinesPendingChunks(this.pendingChunks);
    }
}
