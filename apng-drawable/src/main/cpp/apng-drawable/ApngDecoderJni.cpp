//
// Copyright 2018 LINE Corporation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

#include <jni.h>
#include <unordered_map>
#include <mutex>
#include <android/bitmap.h>
#include "Log.h"
#include "ApngDecoder.h"
#include "Error.h"

#include "StreamSource.h"
#ifdef BUILD_DEBUG
#include<chrono>
#endif

namespace apng_drawable {

static std::unordered_map<int32_t, std::shared_ptr<ApngImage>> gImageMap;
static std::mutex gLock;
static uint32_t gIdCounter;

static jclass gResult_class;
static jfieldID gResult_heightFieldID;
static jfieldID gResult_widthFieldID;
static jfieldID gResult_frameCountFieldID;
static jfieldID gResult_repeatCountFieldID;
static jfieldID gResult_frameDurationsFieldID;
static jfieldID gResult_allFrameByteCountFieldID;

void copyFrameDurations(JNIEnv *env,
                        const std::shared_ptr<ApngImage> &image,
                        jintArray &frame_durations_ptr);

extern "C" {
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-parameter"

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
  JNIEnv *env;
  if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
    return JNI_ERR;
  }
  gIdCounter = 0;

  jclass result_class = env->FindClass("com/linecorp/apng/decoder/Apng$DecodeResult");
  gResult_class = reinterpret_cast<jclass>(env->NewGlobalRef(result_class));
  env->DeleteLocalRef(result_class);
  gResult_heightFieldID = env->GetFieldID(gResult_class, "height", "I");
  gResult_widthFieldID = env->GetFieldID(gResult_class, "width", "I");
  gResult_frameCountFieldID = env->GetFieldID(gResult_class, "frameCount", "I");
  gResult_repeatCountFieldID = env->GetFieldID(gResult_class, "loopCount", "I");
  gResult_frameDurationsFieldID = env->GetFieldID(gResult_class, "frameDurations", "[I");
  gResult_allFrameByteCountFieldID = env->GetFieldID(gResult_class, "allFrameByteCount", "J");
  StreamSource::registerJavaClass(env);
  return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
  JNIEnv *env;
  if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
    return;
  }
  gResult_heightFieldID = nullptr;
  gResult_widthFieldID = nullptr;
  gResult_frameCountFieldID = nullptr;
  gResult_repeatCountFieldID = nullptr;
  gResult_frameDurationsFieldID = nullptr;
  gResult_allFrameByteCountFieldID = nullptr;
  env->DeleteGlobalRef(gResult_class);
  gResult_class = nullptr;
  StreamSource::unregisterJavaClass(env);

  std::lock_guard<std::mutex> lock(gLock);
  gImageMap.clear();
}

JNIEXPORT jint JNICALL
Java_com_linecorp_apng_decoder_ApngDecoderJni_decode(
    JNIEnv *env,
    jclass thiz,
    jobject inputStream,
    jobject result
) {
  LOGV("decode start");
#ifdef BUILD_DEBUG
  std::chrono::system_clock::time_point start;
  std::chrono::system_clock::time_point end;
  start = std::chrono::system_clock::now();
#endif
  int32_t resultCode;

  // decode
  auto source = std::make_unique<StreamSource>(env, inputStream);
  std::shared_ptr<ApngImage> image = std::move(ApngDecoder::decode(std::move(source), resultCode));

  LOGV(" | decode result: %d", resultCode);

  if (resultCode != SUCCESS) {
    return resultCode;
  }

#ifdef BUILD_DEBUG
  end = std::chrono::system_clock::now();
  auto elapsed = std::chrono::duration_cast<std::chrono::nanoseconds>(end - start).count();
  LOGV(" | time: %lld ns (%lld ms)",
       elapsed,
       elapsed / 1000000);
#endif

  LOGV(" | set to field");

  env->SetIntField(result, gResult_widthFieldID, image->getWidth());
  env->SetIntField(result, gResult_heightFieldID, image->getHeight());
  env->SetIntField(result, gResult_frameCountFieldID, image->getFrameCount());
  env->SetIntField(result, gResult_repeatCountFieldID, image->getRepeatCount());
  env->SetLongField(result, gResult_allFrameByteCountFieldID, image->getAllFrameByteCount());
  {
    jintArray frame_durations = env->NewIntArray(image->getFrameCount());
    if (!frame_durations) {
      return ERR_OUT_OF_MEMORY;
    }
    copyFrameDurations(env, image, frame_durations);
    env->SetObjectField(result, gResult_frameDurationsFieldID, frame_durations);
    env->DeleteLocalRef(frame_durations);
  }
  {
    std::lock_guard<std::mutex> lock(gLock);
    gIdCounter++;
    gImageMap.emplace(gIdCounter, std::move(image));
    resultCode = gIdCounter;
    LOGV(" | total images: %ld", gImageMap.size());
  }
  LOGV("decode end");
  return resultCode;
}

JNIEXPORT jboolean JNICALL
Java_com_linecorp_apng_decoder_ApngDecoderJni_isApng(
    JNIEnv *env,
    jclass thiz,
    jobject inputStream
) {
  // check
  auto source = std::make_unique<StreamSource>(env, inputStream);
  bool result = ApngDecoder::isApng(std::move(source));
  return static_cast<jboolean>(result);
}

JNIEXPORT void JNICALL
Java_com_linecorp_apng_decoder_ApngDecoderJni_draw(
    JNIEnv *env,
    jclass thiz,
    jint id,
    jint index,
    jobject bitmap
) {
  if (id < 0 || index < 0) {
    return;
  }

  void *bitmapPixels;
  if (AndroidBitmap_lockPixels(env, bitmap, &bitmapPixels) < 0) {
    LOGE("Error: AndroidBitmap_lockPixels failed.");
    return;
  }

  const void* framePixels = nullptr;
  size_t bytesToCopy = 0;

  {
    std::lock_guard<std::mutex> lock(gLock);
    const auto it = gImageMap.find(id);
    if (it != gImageMap.end()) {
      const auto& image = it->second;
      const auto frame = image->getFrame(static_cast<uint32_t>(index));
      if (frame) {
          framePixels = frame->getRawPixels();
          bytesToCopy = image->getFrameByteCount();
      }
    }
  }

  if (framePixels) {
    memcpy(bitmapPixels, framePixels, bytesToCopy);
  }

  AndroidBitmap_unlockPixels(env, bitmap);
}

JNIEXPORT jint JNICALL
Java_com_linecorp_apng_decoder_ApngDecoderJni_recycle(
    JNIEnv *env,
    jclass thiz,
    jint id
) {
  LOGV("recycle start. id : %d", id);
  if (id < 0) {
    return ERR_NOT_EXIST_IMAGE;
  }
  std::lock_guard<std::mutex> lock(gLock);
  auto const &it = gImageMap.find(id);
  if (it == gImageMap.end()) {
    return ERR_NOT_EXIST_IMAGE;
  }
  gImageMap.erase(it);
  LOGV(" | removed from map. remaining: %ld", gImageMap.size());
  LOGV("recycle end");
  return SUCCESS;
}

JNIEXPORT jint JNICALL
Java_com_linecorp_apng_decoder_ApngDecoderJni_copy(
    JNIEnv *env,
    jclass thiz,
    jint id,
    jobject result
) {
  LOGV("copy start. id : %d", id);
  if (id < 0) {
    return ERR_NOT_EXIST_IMAGE;
  }
  std::lock_guard<std::mutex> lock(gLock);
  auto const &it = gImageMap.find(id);
  if (it == gImageMap.end()) {
    return ERR_NOT_EXIST_IMAGE;
  }

  auto copyPtr = it->second;

  env->SetIntField(result, gResult_widthFieldID, copyPtr->getWidth());
  env->SetIntField(result, gResult_heightFieldID, copyPtr->getHeight());
  env->SetIntField(result, gResult_frameCountFieldID, copyPtr->getFrameCount());
  env->SetIntField(result, gResult_repeatCountFieldID, copyPtr->getRepeatCount());
  env->SetLongField(result, gResult_allFrameByteCountFieldID, copyPtr->getAllFrameByteCount());

  {
    uint32_t frame_count = copyPtr->getFrameCount();
    jintArray frame_durations = env->NewIntArray(frame_count);
    if (!frame_durations) {
      return ERR_OUT_OF_MEMORY;
    }
    copyFrameDurations(env, copyPtr, frame_durations);
    env->SetObjectField(result, gResult_frameDurationsFieldID, frame_durations);
    env->DeleteLocalRef(frame_durations);
  }

  int32_t resultId = ++gIdCounter;
  gImageMap.emplace(resultId, std::move(copyPtr));
  LOGV(" | total images: %ld", gImageMap.size());
  LOGV("copy end");
  return resultId;
}

#pragma clang diagnostic pop
}

void copyFrameDurations(JNIEnv *env,
                        const std::shared_ptr<ApngImage> &image,
                        jintArray &frame_durations_ptr) {
  uint32_t frame_count = image->getFrameCount();
  jint *frame_durations_array = env->GetIntArrayElements(frame_durations_ptr, nullptr);
  for (uint32_t i = 0; i < frame_count; ++i) {
    std::shared_ptr<ApngFrame> frame = image->getFrame(i);
    if (!frame) {
      break;
    }
    frame_durations_array[i] = frame->getDuration();
  }
  env->ReleaseIntArrayElements(frame_durations_ptr, frame_durations_array, 0);
}

}
