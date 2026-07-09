package com.devsgroup.foodmanager.feature.favorites.presentation;

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
public final class FavoritesViewModel_Factory implements Factory<FavoritesViewModel> {
  @Override
  public FavoritesViewModel get() {
    return newInstance();
  }

  public static FavoritesViewModel_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static FavoritesViewModel newInstance() {
    return new FavoritesViewModel();
  }

  private static final class InstanceHolder {
    static final FavoritesViewModel_Factory INSTANCE = new FavoritesViewModel_Factory();
  }
}
