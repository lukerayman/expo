//  Copyright (c) Facebook, Inc. and its affiliates.
//
// This source code is licensed under the MIT license found in the
 // LICENSE file in the root directory of this source tree.

#pragma once

#include <ABI35_0_0jsi/ABI35_0_0jsi.h>
#include <memory.h>

namespace facebook {
namespace jsc {

std::unique_ptr<jsi::Runtime> makeJSCRuntime();

} // namespace jsc
} // namespace facebook
