package com.devsgroup.foodmanager.feature.details.presentation;

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
public final class FoodDetailsViewModel_Factory implements Factory<FoodDetailsViewModel> {
  @Override
  public FoodDetailsViewModel get() {
    return newInstance();
  }

  public static FoodDetailsViewModel_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static FoodDetailsViewModel newInstance() {
    return new FoodDetailsViewModel();
  }

  private static final class InstanceHolder {
    static final FoodDetailsViewModel_Factory INSTANCE = new FoodDetailsViewModel_Factory();
  }
}
