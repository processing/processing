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

#ifndef METHCLA_LOG_H_INCLUDED
#define METHCLA_LOG_H_INCLUDED

typedef enum Methcla_LogLevel
{
    kMethcla_LogError,
    kMethcla_LogWarn,
    kMethcla_LogInfo,
    kMethcla_LogDebug
} Methcla_LogLevel;

typedef struct Methcla_LogHandler
{
    void* handle;
    void (*log_line)(void* handle, Methcla_LogLevel level, const char* message);
} Methcla_LogHandler;

#endif /* METHCLA_LOG_H_INCLUDED */
