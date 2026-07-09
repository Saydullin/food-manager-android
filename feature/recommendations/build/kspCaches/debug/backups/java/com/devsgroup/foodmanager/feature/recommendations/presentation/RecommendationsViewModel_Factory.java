package com.devsgroup.foodmanager.feature.recommendations.presentation;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
@QualifierMetadata
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
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class RecommendationsViewModel_Factory implements Factory<RecommendationsViewModel> {
  @Override
  public RecommendationsViewModel get() {
    return newInstance();
  }

  public static RecommendationsViewModel_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static RecommendationsViewModel newInstance() {
    return new RecommendationsViewModel();
  }

  private static final class InstanceHolder {
    static final RecommendationsViewModel_Factory INSTANCE = new RecommendationsViewModel_Factory();
  }
}
