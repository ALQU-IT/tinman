package dev.alqu.tinman.block;

import com.mojang.serialization.MapCodec;
import dev.alqu.tinman.registry.ModBlockEntities;
import dev.alqu.tinman.registry.ModParticles;
import dev.alqu.tinman.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class AssemblerBlock extends BaseEntityBlock {
	public static final MapCodec<AssemblerBlock> CODEC = simpleCodec(AssemblerBlock::new);

	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	/**
	 * Drives the idle sparks and the hum; set by the block entity while an assembly is running.
	 *
	 * <p>This reuses vanilla's LIT property, so despite the constant's name the blockstate JSON
	 * must key its variants on {@code lit}, which is what blockstates/assembler.json does.
	 */
	public static final BooleanProperty CRAFTING = BlockStateProperties.LIT;

	public AssemblerBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any()
			.setValue(FACING, Direction.NORTH)
			.setValue(CRAFTING, false));
	}

	@Override
	public MapCodec<AssemblerBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, CRAFTING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new AssemblerBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		// Crafting is server-authoritative; the client only animates from the blockstate.
		return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.ASSEMBLER, AssemblerBlockEntity::serverTick);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		if (level.getBlockEntity(pos) instanceof MenuProvider provider) {
			player.openMenu(provider);
		}

		return InteractionResult.CONSUME;
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		Containers.updateNeighboursAfterDestroy(state, level, pos);
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(CRAFTING)) {
			return;
		}

		double x = pos.getX() + 0.5;
		double y = pos.getY() + 0.9;
		double z = pos.getZ() + 0.5;

		if (random.nextDouble() < 0.35) {
			level.addParticle(ModParticles.ASSEMBLER_SPARK,
				x + (random.nextDouble() - 0.5) * 0.6,
				y,
				z + (random.nextDouble() - 0.5) * 0.6,
				0.0, 0.02, 0.0);
		}

		// The hum is client-local so it never costs the server a packet per tick.
		if (random.nextDouble() < 0.06) {
			level.playLocalSound(x, y, z, ModSounds.ASSEMBLER_HUM, SoundSource.BLOCKS, 0.45F, 1.0F, false);
		}
	}
}
