/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#ifdef __cplusplus

#include <memory>

#import <Foundation/Foundation.h>

/**
 * Type erased wrapper over any cxx value that can be passed as an argument
 * to native method.
 */

@interface ABI35_0_0RCTManagedPointer: NSObject

@property (nonatomic, readonly) void *voidPointer;

- (instancetype)initWithPointer:(std::shared_ptr<void>)pointer;

@end

namespace facebook {
namespace ReactABI35_0_0 {

template <typename T, typename P>
ABI35_0_0RCTManagedPointer *managedPointer(P initializer)
{
  auto ptr = std::shared_ptr<void>(new T(initializer));
  return [[ABI35_0_0RCTManagedPointer alloc] initWithPointer:std::move(ptr)];
}

} }

#endif
