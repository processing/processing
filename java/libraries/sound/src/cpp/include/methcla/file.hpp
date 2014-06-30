// Copyright 2012-2013 Samplecount S.L.
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

#ifndef METHCLA_FILE_HPP_INCLUDED
#define METHCLA_FILE_HPP_INCLUDED

#include <methcla/detail.hpp>
#include <methcla/engine.hpp>
#include <methcla/file.h>
#include <methcla/plugin.h>

#include <stdexcept>

namespace Methcla
{
    class SoundFileInfo : public Methcla_SoundFileInfo
    {
    public:
        SoundFileInfo()
        {
            frames      = 0;
            channels    = 0;
            samplerate  = 0;
            file_type   = kMethcla_SoundFileTypeUnknown;
            file_format = kMethcla_SoundFileFormatUnknown;
        }

        SoundFileInfo(const Methcla_SoundFileInfo& info)
        {
            frames      = info.frames;
            channels    = info.channels;
            samplerate  = info.samplerate;
            file_type   = info.file_type;
            file_format = info.file_format;
        }

        int64_t samples() const
        {
            return channels * frames;
        }

        template <typename T> T duration() const
        {
            return (T)frames/(T)samplerate;
        }
    };

    class SoundFile
    {
        Methcla_SoundFile* m_file;
        SoundFileInfo      m_info;

        inline void ensureInitialized() const
        {
            if (!m_file)
                throw std::logic_error("SoundFile has not been initialized");
        }

    public:
        SoundFile()
            : m_file(nullptr)
        {}

        SoundFile(Methcla_SoundFile* file, const Methcla_SoundFileInfo& info)
            : m_file(file)
            , m_info(info)
        {}

        SoundFile(const Engine& engine, const std::string& path)
        {
            detail::checkReturnCode(
                methcla_engine_soundfile_open(engine, path.c_str(), kMethcla_FileModeRead, &m_file, &m_info)
            );
        }

        SoundFile(const Engine& engine, const std::string& path, const SoundFileInfo& info)
            : m_info(info)
        {
            detail::checkReturnCode(
                methcla_engine_soundfile_open(engine, path.c_str(), kMethcla_FileModeWrite, &m_file, &m_info)
            );
        }

        SoundFile(const Methcla_Host* host, const std::string& path)
        {
            detail::checkReturnCode(
                methcla_host_soundfile_open(host, path.c_str(), kMethcla_FileModeRead, &m_file, &m_info)
            );
        }

        SoundFile(const Methcla_Host* host, const std::string& path, const SoundFileInfo& info)
            : m_info(info)
        {
            detail::checkReturnCode(
                methcla_host_soundfile_open(host, path.c_str(), kMethcla_FileModeWrite, &m_file, &m_info)
            );
        }

        // SoundFile is moveable
        SoundFile(SoundFile&& other)
            : m_file(std::move(other.m_file))
            , m_info(std::move(other.m_info))
        {
            other.m_file = nullptr;
        }

        SoundFile& operator=(SoundFile&& other)
        {
            m_file = std::move(other.m_file);
            m_info = std::move(other.m_info);
            other.m_file = nullptr;
            return *this;
        }

        // SoundFile is not copyable
        SoundFile(const SoundFile&) = delete;
        SoundFile& operator=(const SoundFile&) = delete;

        ~SoundFile()
        {
            if (m_file != nullptr)
                methcla_soundfile_close(m_file);
        }

        operator bool() const
        {
            return m_file != nullptr;
        }

        const SoundFileInfo& info() const
        {
            return m_info;
        }

        void close()
        {
            ensureInitialized();
            detail::checkReturnCode(methcla_soundfile_close(m_file));
            m_file = nullptr;
        }

        void seek(int64_t numFrames)
        {
            ensureInitialized();
            detail::checkReturnCode(methcla_soundfile_seek(m_file, numFrames));
        }

        int64_t tell()
        {
            ensureInitialized();
            int64_t numFrames;
            detail::checkReturnCode(methcla_soundfile_tell(m_file, &numFrames));
            return numFrames;
        }

        size_t read(float* buffer, size_t numFrames)
        {
            ensureInitialized();
            size_t outNumFrames;
            detail::checkReturnCode(methcla_soundfile_read_float(m_file, buffer, numFrames, &outNumFrames));
            return outNumFrames;
        }

        size_t write(const float* buffer, size_t numFrames)
        {
            ensureInitialized();
            size_t outNumFrames;
            detail::checkReturnCode(methcla_soundfile_write_float(m_file, buffer, numFrames, &outNumFrames));
            return outNumFrames;
        }
    };
}

#endif // METHCLA_FILE_HPP_INCLUDED