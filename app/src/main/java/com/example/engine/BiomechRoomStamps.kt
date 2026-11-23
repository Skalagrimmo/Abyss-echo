package com.example.engine

import com.example.model.BiomechDecal
import com.example.model.Tile
import com.example.model.TileType
import com.example.model.TilesetBiome
import kotlin.random.Random

/**
 * Hand-crafted architectural tileset stamps for biomechanical BSP chambers.
 * Legend:
 *  '.' = Standard biome floor
 *  '#' = Internal structural pillar / biomechanical bulkhead (never placed on borders/center)
 *  '=' = Rust iron grate / rib catwalk
 *  '~' = Biome hazard or pool (Toxic/Water for Flesh, Oil for Steel, Ember Hearth for Crucible)
 *  '+' = Biomechanical floor conduit node (veins, copper pipes, or molten runic seam)
 *  'R' = Rivet-plated steel floor
 */
data class BiomechStamp(
    val id: String,
    val preferredBiome: TilesetBiome?,
    val pattern: List<String>
) {
    val width: Int get() = pattern.firstOrNull()?.length ?: 0
    val height: Int get() = pattern.size
}

object BiomechRoomStamps {

    val stamps = listOf(
        // FLESH BIOME STAMPS
        BiomechStamp(
            id = "flesh_bio_vat",
            preferredBiome = TilesetBiome.FLESH,
            pattern = listOf(
                "..+.+..",
                ".#~~~#.",
                "+~=+=~+",
                ".~+=+~.",
                "+~=+=~+",
                ".#~~~#.",
                "..+.+.."
            )
        ),
        BiomechStamp(
            id = "flesh_rib_spine",
            preferredBiome = TilesetBiome.FLESH,
            pattern = listOf(
                "...+...",
                ".#=+==#",
                "...+...",
                ".#=+==#",
                "...+..."
            )
        ),
        BiomechStamp(
            id = "flesh_cyst_sanctum",
            preferredBiome = TilesetBiome.FLESH,
            pattern = listOf(
                ".+...+.",
                ".#...#.",
                "..~+~..",
                ".#...#.",
                ".+...+."
            )
        ),

        // STEEL BIOME STAMPS
        BiomechStamp(
            id = "steel_piston_bay",
            preferredBiome = TilesetBiome.STEEL,
            pattern = listOf(
                ".R+++R.",
                ".#===#.",
                "+~===~+",
                ".R===R.",
                "+~===~+",
                ".#===#.",
                ".R+++R."
            )
        ),
        BiomechStamp(
            id = "steel_boiler_manifold",
            preferredBiome = TilesetBiome.STEEL,
            pattern = listOf(
                "..+R+..",
                ".#...#.",
                "+==+==+",
                ".#~~~#.",
                "..+R+.."
            )
        ),
        BiomechStamp(
            id = "steel_catwalk_trench",
            preferredBiome = TilesetBiome.STEEL,
            pattern = listOf(
                ".R...R.",
                ".~===~.",
                "+=====+",
                ".~===~.",
                ".R...R."
            )
        ),

        // CRUCIBLE BIOME STAMPS
        BiomechStamp(
            id = "crucible_smelter_bridge",
            preferredBiome = TilesetBiome.CRUCIBLE,
            pattern = listOf(
                "..+.+..",
                ".#~=~#.",
                "+~===~+",
                ".~=+==~",
                "+~===~+",
                ".#~=~#.",
                "..+.+.."
            )
        ),
        BiomechStamp(
            id = "crucible_furnace_pillars",
            preferredBiome = TilesetBiome.CRUCIBLE,
            pattern = listOf(
                "..+++..",
                ".#...#.",
                "+..~..+",
                ".#...#.",
                "..+++.."
            )
        ),

        // UNIVERSAL TACTICAL COVER STAMP
        BiomechStamp(
            id = "universal_cross_bunker",
            preferredBiome = null,
            pattern = listOf(
                "...+...",
                ".#...#.",
                "..=+=..",
                ".#...#.",
                "...+..."
            )
        )
    )

    fun pickStampFor(biome: TilesetBiome, roomW: Int, roomH: Int, random: Random): BiomechStamp? {
        val candidates = stamps.filter {
            (it.preferredBiome == biome || it.preferredBiome == null) &&
                    it.width <= roomW && it.height <= roomH
        }
        return if (candidates.isNotEmpty()) candidates.random(random) else null
    }

    fun applyStampToRoom(
        tiles: Array<Array<Tile>>,
        roomX: Int,
        roomY: Int,
        roomW: Int,
        roomH: Int,
        biome: TilesetBiome,
        stamp: BiomechStamp,
        random: Random
    ) {
        val startX = roomX + (roomW - stamp.width) / 2
        val startY = roomY + (roomH - stamp.height) / 2
        val centerX = roomX + roomW / 2
        val centerY = roomY + roomH / 2

        for (sy in 0 until stamp.height) {
            val row = stamp.pattern[sy]
            for (sx in 0 until stamp.width) {
                val tx = startX + sx
                val ty = startY + sy
                if (tx !in 1 until tiles.size - 1 || ty !in 1 until tiles[0].size - 1) continue

                val tile = tiles[tx][ty]
                tile.biome = biome

                // Guarantee outer border of room & center coordinate remain walkable
                val isBorder = (tx == roomX || tx == roomX + roomW - 1 || ty == roomY || ty == roomY + roomH - 1)
                val isCenter = (tx == centerX && ty == centerY)

                when (row[sx]) {
                    '#' -> {
                        if (!isBorder && !isCenter) {
                            tile.type = when (biome) {
                                TilesetBiome.FLESH -> if (random.nextFloat() < 0.35f) TileType.IRON_WALL else TileType.WALL
                                TilesetBiome.STEEL -> TileType.IRON_WALL
                                TilesetBiome.CRUCIBLE -> if (random.nextFloat() < 0.65f) TileType.IRON_WALL else TileType.WALL
                            }
                        }
                    }
                    '=' -> {
                        tile.type = TileType.RUST_GRATE
                        tile.decal = when (biome) {
                            TilesetBiome.FLESH -> BiomechDecal.BONE_RIBS
                            TilesetBiome.STEEL -> BiomechDecal.RIVET_PLATE
                            TilesetBiome.CRUCIBLE -> BiomechDecal.RIVET_PLATE
                        }
                    }
                    '~' -> {
                        if (!isCenter) {
                            tile.type = when (biome) {
                                TilesetBiome.FLESH -> if (random.nextFloat() < 0.55f) TileType.TOXIC_MIRE else TileType.WATER_PUDDLE
                                TilesetBiome.STEEL -> TileType.OIL_SLICK
                                TilesetBiome.CRUCIBLE -> TileType.EMBER_HEARTH
                            }
                        }
                    }
                    '+' -> {
                        tile.type = TileType.FLOOR
                        tile.decal = when (biome) {
                            TilesetBiome.FLESH -> BiomechDecal.FLESH_VEINS
                            TilesetBiome.STEEL -> BiomechDecal.COPPER_PIPE
                            TilesetBiome.CRUCIBLE -> BiomechDecal.RUNIC_SEAM
                        }
                    }
                    'R' -> {
                        tile.type = TileType.FLOOR
                        tile.decal = BiomechDecal.RIVET_PLATE
                    }
                    else -> {
                        tile.type = TileType.FLOOR
                    }
                }
            }
        }
    }
}
