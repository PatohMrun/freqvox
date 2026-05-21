package com.freqvox.playback;

import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ServiceComponent;
import dagger.hilt.codegen.OriginatingElement;
import dagger.hilt.internal.GeneratedEntryPoint;
import javax.annotation.processing.Generated;

@OriginatingElement(
    topLevelClass = FreqvoxPlaybackService.class
)
@GeneratedEntryPoint
@InstallIn(ServiceComponent.class)
@Generated("dagger.hilt.android.processor.internal.androidentrypoint.InjectorEntryPointGenerator")
public interface FreqvoxPlaybackService_GeneratedInjector {
  void injectFreqvoxPlaybackService(FreqvoxPlaybackService freqvoxPlaybackService);
}
