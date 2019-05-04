package com.tensorlab.ml;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ValueCategory {
	MEAN("mean"),
	QUANTILE_01("quantile-01"),
	QUANTILE_05("quantile-05"),
	QUANTILE_09("quantile-09"),
	ACTUAL("actual");
	
	private final @NonNull String value;
}
