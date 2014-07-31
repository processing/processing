/*
    Copyright 2012-2013 Samplecount S.L.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

#ifndef METHCLA_COMMON_H_INCLUDED
#define METHCLA_COMMON_H_INCLUDED

#include <stdbool.h>
#include <stddef.h>

#if defined(__cplusplus)
extern "C" {
#endif

#if defined(__cplusplus)
#   define METHCLA_C_LINKAGE extern "C"
#else
#   define METHCLA_C_LINKAGE
#endif

#if defined _WIN32 || defined __CYGWIN__
  #if defined(BUILDING_DLL)
    #if defined(__GNUC__) || defined(__clang__)
      #define METHCLA_VISIBLE __attribute__ ((dllexport))
    #else
      #define METHCLA_VISIBLE __declspec(dllexport) // Note: actually gcc seems to also supports this syntax.
    #endif
  #else
    #if defined(__GNUC__) || defined(__clang__)
      #define METHCLA_VISIBLE __attribute__ ((dllimport))
    #else
      #define METHCLA_VISIBLE __declspec(dllimport) // Note: actually gcc seems to also supports this syntax.
    #endif
  #endif
#else
  #if (__GNUC__ >= 4) || (defined(__clang__) && (__clang_major__ >= 4))
    #define METHCLA_VISIBLE __attribute__ ((visibility ("default")))
  #else
    #define METHCLA_VISIBLE
  #endif
#endif

#define METHCLA_EXPORT METHCLA_C_LINKAGE METHCLA_VISIBLE

//* Time in seconds.
typedef double Methcla_Time;

typedef struct
{
    const void* data;
    size_t      size;
} Methcla_OSCPacket;

typedef enum
{
    kMethcla_NoError = 0,

    /* Generic error codes */
    kMethcla_UnspecifiedError,
    kMethcla_LogicError,
    kMethcla_ArgumentError,
    kMethcla_MemoryError,
    kMethcla_UnimplementedError,
    kMethcla_SystemError,

    /* Engine errors */
    kMethcla_SynthDefNotFoundError = 1000,
    kMethcla_NodeIdError,
    kMethcla_NodeTypeError,

    /* File errors */
    kMethcla_FileNotFoundError = 2000,
    kMethcla_FileExistsError,
    kMethcla_PermissionsError,
    kMethcla_UnsupportedFileTypeError,
    kMethcla_UnsupportedDataFormatError,
    kMethcla_InvalidFileError,

    /* Audio driver errors */
    kMethcla_DeviceUnavailableError = 3000,
} Methcla_ErrorCode;

METHCLA_EXPORT const char* methcla_error_code_description(Methcla_ErrorCode code);

typedef struct Methcla_Error
{
    Methcla_ErrorCode error_code;
    char*             error_message;
} Methcla_Error;

static inline bool methcla_is_ok(const Methcla_Error error)
{
    return error.error_code == kMethcla_NoError;
}

static inline bool methcla_is_error(const Methcla_Error error)
{
    return error.error_code != kMethcla_NoError;
}

static inline bool methcla_error_has_code(const Methcla_Error error, Methcla_ErrorCode code)
{
    return error.error_code == code;
}

static inline Methcla_ErrorCode methcla_error_code(const Methcla_Error error)
{
    return error.error_code;
}

static inline const char* methcla_error_message(const Methcla_Error error)
{
    return error.error_message;
}

//* Create a new Methcla_Error with a specific error code.
//  The error message is set to NULL.
METHCLA_EXPORT Methcla_Error methcla_error_new(Methcla_ErrorCode code);

//* Create a new Methcla_Error with a specific error code and message.
METHCLA_EXPORT Methcla_Error methcla_error_new_with_message(Methcla_ErrorCode code, const char* message);

//* Free the resources associated with a Methcla_Error.
METHCLA_EXPORT void methcla_error_free(Methcla_Error error);

//* Return a Methcla_Error indicating that no error has occurred.
static inline Methcla_Error methcla_no_error()
{
    return methcla_error_new(kMethcla_NoError);
}

//* Audio sample type
typedef float Methcla_AudioSample;

METHCLA_EXPORT void methcla_init();

#if defined(__cplusplus)
}
#endif

#endif /* METHCLA_COMMON_H_INCLUDED */
