// Code generated by protoc-gen-go. DO NOT EDIT.
// source: helloworld.proto

/*
Package helloworld is a generated protocol buffer package.

It is generated from these files:
	helloworld.proto

It has these top-level messages:
	HelloRequest
	HelloReply
*/
package helloworld

import proto "github.com/golang/protobuf/proto"
import fmt "fmt"
import math "math"

import (
	context "golang.org/x/net/context"
	grpc "google.golang.org/grpc"
)

// Reference imports to suppress errors if they are not otherwise used.
var _ = proto.Marshal
var _ = fmt.Errorf
var _ = math.Inf

// This is a compile-time assertion to ensure that this generated file
// is compatible with the proto package it is being compiled against.
// A compilation error at this line likely means your copy of the
// proto package needs to be updated.
const _ = proto.ProtoPackageIsVersion2 // please upgrade the proto package

type HelloRequest struct {
	Name string `protobuf:"bytes,1,opt,name=name" json:"name,omitempty"`
}

func (m *HelloRequest) Reset()                    { *m = HelloRequest{} }
func (m *HelloRequest) String() string            { return proto.CompactTextString(m) }
func (*HelloRequest) ProtoMessage()               {}
func (*HelloRequest) Descriptor() ([]byte, []int) { return fileDescriptor0, []int{0} }

func (m *HelloRequest) GetName() string {
	if m != nil {
		return m.Name
	}
	return ""
}

// The response message containing the greetings
type HelloReply struct {
	Message string `protobuf:"bytes,1,opt,name=message" json:"message,omitempty"`
}

func (m *HelloReply) Reset()                    { *m = HelloReply{} }
func (m *HelloReply) String() string            { return proto.CompactTextString(m) }
func (*HelloReply) ProtoMessage()               {}
func (*HelloReply) Descriptor() ([]byte, []int) { return fileDescriptor0, []int{1} }

func (m *HelloReply) GetMessage() string {
	if m != nil {
		return m.Message
	}
	return ""
}

func init() {
	proto.RegisterType((*HelloRequest)(nil), "helloworld.HelloRequest")
	proto.RegisterType((*HelloReply)(nil), "helloworld.HelloReply")
}

// Reference imports to suppress errors if they are not otherwise used.
var _ context.Context
var _ grpc.ClientConn

// This is a compile-time assertion to ensure that this generated file
// is compatible with the grpc package it is being compiled against.
const _ = grpc.SupportPackageIsVersion4

// Client API for Greeter service

type GreeterClient interface {
	SayHelloStream(ctx context.Context, opts ...grpc.CallOption) (Greeter_SayHelloStreamClient, error)
	SayHello(ctx context.Context, in *HelloRequest, opts ...grpc.CallOption) (*HelloReply, error)
}

type greeterClient struct {
	cc *grpc.ClientConn
}

func NewGreeterClient(cc *grpc.ClientConn) GreeterClient {
	return &greeterClient{cc}
}

func (c *greeterClient) SayHelloStream(ctx context.Context, opts ...grpc.CallOption) (Greeter_SayHelloStreamClient, error) {
	stream, err := grpc.NewClientStream(ctx, &_Greeter_serviceDesc.Streams[0], c.cc, "/helloworld.Greeter/SayHelloStream", opts...)
	if err != nil {
		return nil, err
	}
	x := &greeterSayHelloStreamClient{stream}
	return x, nil
}

type Greeter_SayHelloStreamClient interface {
	Send(*HelloRequest) error
	Recv() (*HelloReply, error)
	grpc.ClientStream
}

type greeterSayHelloStreamClient struct {
	grpc.ClientStream
}

func (x *greeterSayHelloStreamClient) Send(m *HelloRequest) error {
	return x.ClientStream.SendMsg(m)
}

func (x *greeterSayHelloStreamClient) Recv() (*HelloReply, error) {
	m := new(HelloReply)
	if err := x.ClientStream.RecvMsg(m); err != nil {
		return nil, err
	}
	return m, nil
}

func (c *greeterClient) SayHello(ctx context.Context, in *HelloRequest, opts ...grpc.CallOption) (*HelloReply, error) {
	out := new(HelloReply)
	err := grpc.Invoke(ctx, "/helloworld.Greeter/SayHello", in, out, c.cc, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

// Server API for Greeter service

type GreeterServer interface {
	SayHelloStream(Greeter_SayHelloStreamServer) error
	SayHello(context.Context, *HelloRequest) (*HelloReply, error)
}

func RegisterGreeterServer(s *grpc.Server, srv GreeterServer) {
	s.RegisterService(&_Greeter_serviceDesc, srv)
}

func _Greeter_SayHelloStream_Handler(srv interface{}, stream grpc.ServerStream) error {
	return srv.(GreeterServer).SayHelloStream(&greeterSayHelloStreamServer{stream})
}

type Greeter_SayHelloStreamServer interface {
	Send(*HelloReply) error
	Recv() (*HelloRequest, error)
	grpc.ServerStream
}

type greeterSayHelloStreamServer struct {
	grpc.ServerStream
}

func (x *greeterSayHelloStreamServer) Send(m *HelloReply) error {
	return x.ServerStream.SendMsg(m)
}

func (x *greeterSayHelloStreamServer) Recv() (*HelloRequest, error) {
	m := new(HelloRequest)
	if err := x.ServerStream.RecvMsg(m); err != nil {
		return nil, err
	}
	return m, nil
}

func _Greeter_SayHello_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(HelloRequest)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(GreeterServer).SayHello(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/helloworld.Greeter/SayHello",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(GreeterServer).SayHello(ctx, req.(*HelloRequest))
	}
	return interceptor(ctx, in, info, handler)
}

var _Greeter_serviceDesc = grpc.ServiceDesc{
	ServiceName: "helloworld.Greeter",
	HandlerType: (*GreeterServer)(nil),
	Methods: []grpc.MethodDesc{
		{
			MethodName: "SayHello",
			Handler:    _Greeter_SayHello_Handler,
		},
	},
	Streams: []grpc.StreamDesc{
		{
			StreamName:    "SayHelloStream",
			Handler:       _Greeter_SayHelloStream_Handler,
			ServerStreams: true,
			ClientStreams: true,
		},
	},
	Metadata: "helloworld.proto",
}

func init() { proto.RegisterFile("helloworld.proto", fileDescriptor0) }

var fileDescriptor0 = []byte{
	// 195 bytes of a gzipped FileDescriptorProto
	0x1f, 0x8b, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0xff, 0xe2, 0x12, 0xc8, 0x48, 0xcd, 0xc9,
	0xc9, 0x2f, 0xcf, 0x2f, 0xca, 0x49, 0xd1, 0x2b, 0x28, 0xca, 0x2f, 0xc9, 0x17, 0xe2, 0x42, 0x88,
	0x28, 0x29, 0x71, 0xf1, 0x78, 0x80, 0x78, 0x41, 0xa9, 0x85, 0xa5, 0xa9, 0xc5, 0x25, 0x42, 0x42,
	0x5c, 0x2c, 0x79, 0x89, 0xb9, 0xa9, 0x12, 0x8c, 0x0a, 0x8c, 0x1a, 0x9c, 0x41, 0x60, 0xb6, 0x92,
	0x1a, 0x17, 0x17, 0x54, 0x4d, 0x41, 0x4e, 0xa5, 0x90, 0x04, 0x17, 0x7b, 0x6e, 0x6a, 0x71, 0x71,
	0x62, 0x3a, 0x4c, 0x11, 0x8c, 0x6b, 0x34, 0x99, 0x91, 0x8b, 0xdd, 0xbd, 0x28, 0x35, 0xb5, 0x24,
	0xb5, 0x48, 0xc8, 0x83, 0x8b, 0x2f, 0x38, 0xb1, 0x12, 0xac, 0x2d, 0xb8, 0xa4, 0x28, 0x35, 0x31,
	0x57, 0x48, 0x42, 0x0f, 0xc9, 0x21, 0xc8, 0x76, 0x4a, 0x89, 0x61, 0x91, 0x29, 0xc8, 0xa9, 0x54,
	0x62, 0xd0, 0x60, 0x34, 0x60, 0x14, 0xb2, 0xe3, 0xe2, 0x80, 0x99, 0x44, 0x8e, 0x19, 0x4e, 0x06,
	0x5c, 0xd2, 0x99, 0xf9, 0x7a, 0xe9, 0x45, 0x05, 0xc9, 0x7a, 0xa9, 0x15, 0x89, 0xb9, 0x05, 0x39,
	0xa9, 0xc5, 0x48, 0x6a, 0x9d, 0xf8, 0xc1, 0x8a, 0xc3, 0x41, 0xec, 0x00, 0x50, 0xe8, 0x04, 0x30,
	0x26, 0xb1, 0x81, 0x83, 0xc9, 0x18, 0x10, 0x00, 0x00, 0xff, 0xff, 0x99, 0x4d, 0xbf, 0xb0, 0x3a,
	0x01, 0x00, 0x00,
}
