package app.revanced.patches.youtube.general.trendingsearches.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.general.trendingsearches.fingerprints.SearchBarEntryFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.YtOutlineArrowTimeBlack
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.YtOutlineFireBlack
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.YtOutlineSearchBlack
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.integrations.Constants.GENERAL
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction

@Patch
@Name("hide-trending-searches")
@Description("Hide trending searches in the search bar.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class TrendingSearchesPatch : BytecodePatch(
    listOf(SearchBarEntryFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        SearchBarEntryFingerprint.result?.let {
            it.mutableMethod.apply {
                SearchTerm.values()
                    .map { searchTerm -> getWideLiteralIndex(searchTerm.resourceId) to searchTerm.value }
                    .sortedBy { searchTerm -> searchTerm.first }
                    .reversed()
                    .forEach { (index, value) ->
                        val freeRegister = getInstruction<OneRegisterInstruction>(index).registerA
                        val viewRegister =
                            getInstruction<TwoRegisterInstruction>(index - 1).registerA

                        addInstructions(
                            index, """
                                const/4 v$freeRegister, $value
                                invoke-static {v$viewRegister, v$freeRegister}, $GENERAL->hideTrendingSearches(Landroid/widget/ImageView;Z)V
                                """
                        )
                    }
            }
        } ?: return SearchBarEntryFingerprint.toErrorResult()

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_SETTINGS",
                "SETTINGS: HIDE_TRENDING_SEARCHES"
            )
        )

        SettingsPatch.updatePatchStatus("hide-trending-searches")

        return PatchResultSuccess()
    }

    private enum class SearchTerm(val resourceId: Long, val value: Int) {
        HISTORY(YtOutlineArrowTimeBlack, 0),
        SEARCH(YtOutlineSearchBlack, 0),
        TRENDING(YtOutlineFireBlack, 1)
    }
}