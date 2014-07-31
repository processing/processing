// Copyright 2014 Samplecount S.L.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

#ifndef METHCLA_LOG_HPP_INCLUDED
#define METHCLA_LOG_HPP_INCLUDED

#include <methcla/log.h>

#include <functional>
#include <memory>
#include <sstream>

namespace Methcla {

class LogStream
{
    Methcla_LogLevel                                  m_level;
    std::function<void(Methcla_LogLevel,const char*)> m_callback;
    std::stringstream*                                m_stream;

public:
    LogStream(std::function<void(Methcla_LogLevel,const char*)> callback, Methcla_LogLevel messageLevel, Methcla_LogLevel currentLevel)
        : m_level(messageLevel)
        , m_callback(messageLevel <= currentLevel ? callback : nullptr)
        , m_stream(nullptr)
    {}

    LogStream(std::function<void(Methcla_LogLevel,const char*)> callback, Methcla_LogLevel messageLevel)
        : LogStream(callback, messageLevel, messageLevel)
    {}

    LogStream(const LogStream& other)
        : m_level(other.m_level)
        , m_callback(other.m_callback)
        , m_stream(other.m_stream ? new std::stringstream(other.m_stream->str()) : nullptr)
    {}

    ~LogStream()
    {
        if (m_stream)
        {
            try
            {
                if (m_callback)
                    m_callback(m_level, m_stream->str().c_str());
                delete m_stream;
            }
            catch (...)
            {
                delete m_stream;
                throw;
            }
        }
    }

    template <class T> LogStream& operator<<(const T& x)
    {
        if (m_callback)
        {
            if (!m_stream)
                m_stream = new std::stringstream();
            *m_stream << x;
        }
        return *this;
    }
};

} // namespace Methcla

#endif // METHCLA_LOG_HPP_INCLUDED
