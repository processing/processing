// oscpp library
//
// Copyright (c) 2004-2013 Stefan Kersten <sk@k-hornz.de>
//
// Permission is hereby granted, free of charge, to any person or organization
// obtaining a copy of the software and accompanying documentation covered by
// this license (the "Software") to use, reproduce, display, distribute,
// execute, and transmit the Software, and to prepare derivative works of the
// Software, and to permit third-parties to whom the Software is furnished to
// do so, all subject to the following:
//
// The copyright notices in the Software and this entire statement, including
// the above license grant, this restriction and the following disclaimer,
// must be included in all copies of the Software, in whole or in part, and
// all derivative works of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE, TITLE AND NON-INFRINGEMENT. IN NO EVENT
// SHALL THE COPYRIGHT HOLDERS OR ANYONE DISTRIBUTING THE SOFTWARE BE LIABLE
// FOR ANY DAMAGES OR OTHER LIABILITY, WHETHER IN CONTRACT, TORT OR OTHERWISE,
// ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
// DEALINGS IN THE SOFTWARE.

#ifndef OSCPP_HOST_HPP_INCLUDED
#define OSCPP_HOST_HPP_INCLUDED

#include <cstdint>
#include <oscpp/detail/endian.hpp>
#include <stdexcept>

namespace OSCPP
{
#if defined(__GNUC__)
    inline static uint32_t bswap32(uint32_t x)
    {
        return __builtin_bswap32(x);
    }
    inline static uint64_t bswap64(uint64_t x)
    {
        return __builtin_bswap64(x);
    }
#elif defined(_WINDOWS_)
#   include <stdlib.h>
    inline static uint32_t bswap32(uint32_t x)
    {
        return _byteswap_ulong(x);
    }
    inline static uint64_t bswap64(uint64_t x)
    {
        return _byteswap_uint64(x);
    }
#else
    // Fallback implementation
#   warning Using unoptimized byte swap functions

    inline static uint32_t bswap32(uint32_t x)
    {
        const uint32_t b1 = x << 24;
        const uint32_t b2 = (x & 0x0000FF00) << 8;
        const uint32_t b3 = (x & 0x00FF0000) >> 8;
        const uint32_t b4 = x >> 24;
        return b1 | b2 | b3 | b4;
    }
    inline static uint64_t bswap64(int64_t x)
    {
        const uint64_t w1 = oscpp_bswap(uint32_t(x & 0x00000000FFFFFFFF)) << 32;
        const uint64_t w2 = oscpp_bswap(uint32_t(x >> 32));
        return w1 | w2;
    }
#endif

    enum ByteOrder
    {
        NetworkByteOrder,
        HostByteOrder
    };

    template<ByteOrder B> inline uint32_t convert32(uint32_t)
    {
        throw std::logic_error("Invalid byte order");
    }

    template<> inline uint32_t convert32<NetworkByteOrder>(uint32_t x)
    {
#if defined(OSCPP_LITTLE_ENDIAN)
        return bswap32(x);
#else
        return x;
#endif
    }

    template<> inline uint32_t convert32<HostByteOrder>(uint32_t x)
    {
        return x;
    }

    template<ByteOrder B> inline uint64_t convert64(uint64_t)
    {
        throw std::logic_error("Invalid byte order");
    }

    template<> inline uint64_t convert64<NetworkByteOrder>(uint64_t x)
    {
#if defined(OSCPP_LITTLE_ENDIAN)
        return bswap64(x);
#else
        return x;
#endif
    }

    template<> inline uint64_t convert64<HostByteOrder>(uint64_t x)
    {
        return x;
    }
}

#endif // OSCPP_HOST_HPP_INCLUDED
