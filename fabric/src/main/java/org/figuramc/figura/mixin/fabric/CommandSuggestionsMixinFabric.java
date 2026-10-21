package org.figuramc.figura.mixin.fabric;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.gui.components.CommandSuggestions;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.ducks.SuggestionsListAccessor;
import org.figuramc.figura.font.Emojis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

@Mixin(CommandSuggestions.class)
public abstract class CommandSuggestionsMixinFabric {

    @Shadow private CommandSuggestions.SuggestionsList suggestions;

    @Shadow public abstract void showSuggestions(boolean narrateFirstSuggestion);

    @Unique private boolean emojiSuggestions;

    @Inject(method = "updateCommandInfo", at = @At("HEAD"))
    private void figura$resetSuggestions(CallbackInfo ci) {
        emojiSuggestions = false;
    }

    @WrapOperation(method = "updateCommandInfo", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/SharedSuggestionProvider;suggest(Ljava/lang/Iterable;Lcom/mojang/brigadier/suggestion/SuggestionsBuilder;)Ljava/util/concurrent/CompletableFuture;"))
    private CompletableFuture<Suggestions> addFiguraSuggestions(Iterable<String> suggestions, SuggestionsBuilder builder, Operation<CompletableFuture<Suggestions>> original) {
        if (Configs.EMOJIS.value == 0) {
            return original.call(suggestions, builder);
        }
        Collection<String> emojis = Emojis.getMatchingEmojis(builder.getRemaining());
        emojiSuggestions = !emojis.isEmpty();
        Collection<String> combined = new ArrayList<>();
        suggestions.forEach(combined::add);
        combined.addAll(emojis);
        return original.call(combined, builder);
    }

    @Inject(method = "updateCommandInfo", at = @At("TAIL"))
    private void afterSuggesting(CallbackInfo ci) {
        if (emojiSuggestions && Configs.EMOJIS.value >= 2) {
            this.showSuggestions(false);
        }
    }

    @Inject(method = "showSuggestions", at = @At("RETURN"))
    private void showSuggestions(CallbackInfo ci) {
        if (emojiSuggestions && this.suggestions != null) {
            ((SuggestionsListAccessor) this.suggestions).figura$setFiguraList(true);
            emojiSuggestions = false;
        }
    }
}
