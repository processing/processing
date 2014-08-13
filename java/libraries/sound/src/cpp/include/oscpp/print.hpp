// OSCpp library
//
// Copyright (c) 2004-2011 Stefan Kersten <sk@k-hornz.de>
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

#ifndef OSCPP_PRINT_HPP_INCLUDED
#define OSCPP_PRINT_HPP_INCLUDED

#include <oscpp/client.hpp>
#include <oscpp/server.hpp>
#include <ostream>

namespace OSCPP {
namespace detail {

const size_t kDefaultIndentWidth = 4;

class Indent
{
public:
    Indent(size_t w)
        : m_width(w)
        , m_indent(0)
    { }
    Indent(size_t w, size_t n)
        : m_width(w)
        , m_indent(n)
    { }
    Indent(const Indent&) = default;

    operator size_t () const { return m_indent; }
    Indent inc() const { return Indent(m_width, m_indent+m_width); }

private:
    size_t m_width;
    size_t m_indent;
};

inline std::ostream& operator<<(std::ostream& out, const Indent& indent)
{
    size_t n = indent;
    while (n-- > 0) out << ' ';
    return out;
}

inline void printArgs(std::ostream& out, Server::ArgStream args)
{
    while (!args.atEnd()) {
        const char t = args.tag();
        switch (t) {
            case 'i':
                out << "i:" << args.int32();
                break;
            case 'f':
                out << "f:" << args.float32();
                break;
            case 's':
                out << "s:" << args.string();
                break;
            case 'b':
                out << "b:" << args.blob().size();
                break;
            case '[':
                out << "[ ";
                printArgs(out, args.array());
                out << " ]";
                break;
            default:
                out << t << ":?";
                args.drop();
                break;
        }
        out << ' ';
    }
}

inline void printMessage(std::ostream& out, const Server::Message& msg, const Indent& indent)
{
    out << indent << msg.address() << ' ';
    printArgs(out, msg.args());
}

inline void printBundle(std::ostream& out, const Server::Bundle& bundle, const Indent& indent)
{
    out << indent << "# " << bundle.time() << " [" << std::endl;
    Indent nextIndent = indent.inc();
    auto packets = bundle.packets();
    while (!packets.atEnd()) {
        auto packet = packets.next();
        if (packet.isMessage()) {
            printMessage(out, packet, nextIndent);
        } else {
            printBundle(out, packet, nextIndent);
        }
        out << std::endl;
    }
    out << indent << "]";
}

inline void printPacket(std::ostream& out, const Server::Packet& packet, const Indent& indent)
{
    if (packet.isMessage()) {
        printMessage(out, packet, indent);
    } else {
        printBundle(out, packet, indent);
    }
}

}
}

namespace OSCPP {
namespace Server {

inline std::ostream& operator<<(std::ostream& out, const Packet& packet)
{
    detail::printPacket(out, packet, detail::Indent(detail::kDefaultIndentWidth));
    return out;
}

inline std::ostream& operator<<(std::ostream& out, const Bundle& packet)
{
    detail::printBundle(out, packet, detail::Indent(detail::kDefaultIndentWidth));
    return out;
}

inline std::ostream& operator<<(std::ostream& out, const Message& packet)
{
    detail::printMessage(out, packet, detail::Indent(detail::kDefaultIndentWidth));
    return out;
}

}
}

namespace OSCPP {
namespace Client {

inline std::ostream& operator<<(std::ostream& out, const Packet& packet)
{
    return out << Server::Packet(packet.data(), packet.size());
}

}
}

#endif // OSCPP_PRINT_HPP_INCLUDED
