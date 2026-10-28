package com.example.yogaapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.yogaapp.domain.model.BodyFocus
import com.example.yogaapp.domain.model.PoseSpec
import com.example.yogaapp.domain.model.YogaLevel
import com.example.yogaapp.domain.model.YogaPose

/**
 * Persisted pose row. The PoseSpec is flattened into individual columns because Room doesn't
 * natively serialise nested data classes (and flattening keeps queries/indexes meaningful).
 */
@Entity(tableName = "poses")
data class PoseEntity(
    @PrimaryKey val id: String,
    val sanskritName: String,
    val ruName: String,
    val enName: String,
    val ruDescription: String,
    val enDescription: String,
    val level: YogaLevel,
    val focuses: List<BodyFocus>,
    val defaultDurationSec: Int,
    val torsoTilt: Float,
    val hipAngle: Float,
    val upperLegAngle: Float,
    val lowerLegAngle: Float,
    val upperArmAngle: Float,
    val lowerArmAngle: Float,
    val legsCrossed: Boolean,
    val kneeling: Boolean,
    val armsOverhead: Boolean,
    val armsInPrayer: Boolean,
    val audioAssetName: String?,
) {
    fun toDomain(): YogaPose = YogaPose(
        id = id,
        sanskritName = sanskritName,
        ruName = ruName,
        enName = enName,
        ruDescription = ruDescription,
        enDescription = enDescription,
        level = level,
        focuses = focuses,
        defaultDurationSec = defaultDurationSec,
        spec = PoseSpec(
            torsoTilt = torsoTilt,
            hipAngle = hipAngle,
            upperLegAngle = upperLegAngle,
            lowerLegAngle = lowerLegAngle,
            upperArmAngle = upperArmAngle,
            lowerArmAngle = lowerArmAngle,
            legsCrossed = legsCrossed,
            kneeling = kneeling,
            armsOverhead = armsOverhead,
            armsInPrayer = armsInPrayer,
        ),
        audioAssetName = audioAssetName,
    )

    companion object {
        fun fromDomain(p: YogaPose): PoseEntity = PoseEntity(
            id = p.id,
            sanskritName = p.sanskritName,
            ruName = p.ruName,
            enName = p.enName,
            ruDescription = p.ruDescription,
            enDescription = p.enDescription,
            level = p.level,
            focuses = p.focuses,
            defaultDurationSec = p.defaultDurationSec,
            torsoTilt = p.spec.torsoTilt,
            hipAngle = p.spec.hipAngle,
            upperLegAngle = p.spec.upperLegAngle,
            lowerLegAngle = p.spec.lowerLegAngle,
            upperArmAngle = p.spec.upperArmAngle,
            lowerArmAngle = p.spec.lowerArmAngle,
            legsCrossed = p.spec.legsCrossed,
            kneeling = p.spec.kneeling,
            armsOverhead = p.spec.armsOverhead,
            armsInPrayer = p.spec.armsInPrayer,
            audioAssetName = p.audioAssetName,
        )
    }
}
