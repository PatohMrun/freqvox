package com.freqvox.ui.radio;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class RadioViewModel_Factory implements Factory<RadioViewModel> {
  private final Provider<Context> contextProvider;

  public RadioViewModel_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public RadioViewModel get() {
    return newInstance(contextProvider.get());
  }

  public static RadioViewModel_Factory create(Provider<Context> contextProvider) {
    return new RadioViewModel_Factory(contextProvider);
  }

  public static RadioViewModel newInstance(Context context) {
    return new RadioViewModel(context);
  }
}
