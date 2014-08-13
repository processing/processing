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

#ifndef OSCPP_CLIENT_HPP_INCLUDED
#define OSCPP_CLIENT_HPP_INCLUDED

#include <oscpp/detail/host.hpp>
#include <oscpp/detail/stream.hpp>
#include <oscpp/util.hpp>

#include <cstdint>
#include <limits>
#include <stdexcept>
#include <type_traits>

namespace OSCPP {
namespace Client {

//! OSC packet construction.
/*!
 * Construct a valid OSC packet for transmitting over a transport medium.
 */
class Packet
{
    int32_t calcSize(const char* begin, const char* end)
    {
        // TODO: Make sure pointer difference fits into int32_t
        return end - begin - 4;
    }

public:
    //! Constructor.
    /*!
    */
    Packet()
    {
        reset(0, 0);
    }

    //! Constructor.
    /*!
    */
    Packet(void* buffer, size_t size)
    {
        reset(buffer, size);
    }

    //! Destructor.
    virtual ~Packet() { }

    //! Get packet buffer address.
    /*!
     * Return the start address of the packet currently under construction.
     */
    void* data() const
    {
        return m_buffer;
    }

    size_t capacity() const
    {
        return m_capacity;
    }

    //! Get packet content size.
    /*!
     * Return the size of the packet currently under construction.
     */
    size_t size() const
    {
        return m_args.consumed();
    }

    //! Reset packet state.
    void reset(void* buffer, size_t size)
    {
        checkAlignment(&m_buffer, kAlignment);
        m_buffer = buffer;
        m_capacity = size;
        m_args = WriteStream(m_buffer, m_capacity);
        m_sizePosM = m_sizePosB = nullptr;
        m_inBundle = 0;
    }

    void reset()
    {
        reset(m_buffer, m_capacity);
    }

    Packet& openBundle(uint64_t time)
    {
        if (m_inBundle > 0) {
            // Remember previous size pos offset
            // TODO: Make sure pointer difference fits into int32_t
            const int32_t offset = m_sizePosB - m_args.begin();
            char* curPos = m_args.pos();
            m_args.skip(4);
            // Record size pos
            std::memcpy(curPos, &offset, 4);
            m_sizePosB = curPos;
        } else if (m_args.pos() != m_args.begin()) {
            throw std::logic_error("Cannot open toplevel bundle in non-empty packet");
        }

        m_inBundle++;
        m_args.putString("#bundle");
        m_args.putUInt64(time);
        return *this;
    }

    Packet& closeBundle()
    {
        if (m_inBundle > 0) {
            if (m_inBundle > 1) {
                // Get current stream pos
                char* curPos = m_args.pos();

                // Get previous bundle size stream pos
                int32_t offset;
                memcpy(&offset, m_sizePosB, 4);
                // Get previous size pos
                char* prevPos = m_args.begin() + offset;

                const int32_t bundleSize = calcSize(m_sizePosB, curPos);
                assert(bundleSize >= 0 && (size_t)bundleSize >= Size::bundle(0));
                // Write bundle size
                m_args.setPos(m_sizePosB);
                m_args.putInt32(bundleSize);
                m_args.setPos(curPos);

                // record outer bundle size pos
                m_sizePosB = prevPos;
            }
            m_inBundle--;
        } else {
            throw std::logic_error("closeBundle() without matching openBundle()");
        }
        return *this;
    }

    Packet& openMessage(const char* addr, size_t numTags)
    {
        if (m_inBundle > 0) {
            // record message size pos
            m_sizePosM = m_args.pos();
            // advance arg stream
            m_args.skip(4);
        }
        m_args.putString(addr);
        size_t sigLen = numTags + 2;
        m_tags = WriteStream(m_args, sigLen);
        m_args.zero(align(sigLen));
        m_tags.putChar(',');
        return *this;
    }

    Packet& closeMessage()
    {
        if (m_inBundle > 0) {
            // Get current stream pos
            char* curPos = m_args.pos();
            // write message size
            m_args.setPos(m_sizePosM);
            m_args.putInt32(calcSize(m_sizePosM, curPos));
            // restore stream pos
            m_args.setPos(curPos);
            // reset tag stream
            m_tags = WriteStream();
        }
        return *this;
    }

    //! Write integer message argument.
    /*!
     * Write a 32 bit integer message argument.
     *
     * \param arg 32 bit integer argument.
     *
     * \pre openMessage must have been called before with no intervening
     * closeMessage.
     *
     * \throw OSCPP::XRunError stream buffer xrun.
     */
    Packet& int32(int32_t arg)
    {
        m_tags.putChar('i');
        m_args.putInt32(arg);
        return *this;
    }

    Packet& float32(float arg)
    {
        m_tags.putChar('f');
        m_args.putFloat32(arg);
        return *this;
    }

    Packet& string(const char* arg)
    {
        m_tags.putChar('s');
        m_args.putString(arg);
        return *this;
    }

    // @throw std::invalid_argument if blob size is greater than std::numeric_limits<int32_t>::max()
    Packet& blob(const Blob& arg)
    {
        if (arg.size() > (size_t)std::numeric_limits<int32_t>::max())
            throw std::invalid_argument("Blob size greater than maximum value representable by int32_t");
        m_tags.putChar('b');
        m_args.putInt32(arg.size());
        m_args.putData(arg.data(), arg.size());
        return *this;
    }

    Packet& openArray()
    {
        m_tags.putChar('[');
        return *this;
    }

    Packet& closeArray()
    {
        m_tags.putChar(']');
        return *this;
    }

    template <typename T> Packet& put(T)
    {
        T::OSC_Client_Packet_put_unimplemented;
        return *this;
    }

    template <typename InputIterator> Packet& put(InputIterator begin, InputIterator end)
    {
        for (auto it = begin; it != end; it++) {
            put(*it);
        }
        return *this;
    }

    template <typename InputIterator> Packet& putArray(InputIterator begin, InputIterator end)
    {
        openArray();
        put<InputIterator>(begin, end);
        closeArray();
        return *this;
    }

private:
    void*       m_buffer;
    size_t      m_capacity;
    WriteStream m_args;         // packet stream
    WriteStream m_tags;         // current tag stream
    char*       m_sizePosM;     // last message size position
    char*       m_sizePosB;     // last bundle size position
    size_t      m_inBundle;     // bundle nesting depth
};

template <> inline Packet& Packet::put<int32_t>(int32_t x) { return int32(x); }
template <> inline Packet& Packet::put<float>(float x) { return float32(x); }
template <> inline Packet& Packet::put<const char*>(const char* x) { return string(x); }
template <> inline Packet& Packet::put<Blob>(Blob x) { return blob(x); }

template <size_t buffer_size> class StaticPacket : public Packet
{
public:
    StaticPacket()
        : Packet(reinterpret_cast<char*>(&m_buffer), buffer_size)
    { }

private:
    typedef typename std::aligned_storage<buffer_size,kAlignment>::type AlignedBuffer;
    AlignedBuffer m_buffer;
};

class DynamicPacket : public Packet
{
public:
    DynamicPacket(size_t buffer_size)
        : Packet(static_cast<char*>(new char[buffer_size]), buffer_size)
    { }

    ~DynamicPacket()
    {
        delete [] static_cast<char*>(data());
    }
};

}
}

#endif // OSCPP_CLIENT_HPP_INCLUDED
