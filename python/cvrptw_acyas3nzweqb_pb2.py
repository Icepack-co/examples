# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: cvrptw-acyas3nzweqb.proto

import sys
_b=sys.version_info[0]<3 and (lambda x:x) or (lambda x:x.encode('latin1'))
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from google.protobuf import reflection as _reflection
from google.protobuf import symbol_database as _symbol_database
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()




DESCRIPTOR = _descriptor.FileDescriptor(
  name='cvrptw-acyas3nzweqb.proto',
  package='CVRPTW',
  syntax='proto2',
  serialized_options=None,
  serialized_pb=_b('\n\x19\x63vrptw-acyas3nzweqb.proto\x12\x06\x43VRPTW\"h\n\x07Geocode\x12\n\n\x02id\x18\x01 \x02(\t\x12\t\n\x01x\x18\x02 \x02(\x02\x12\t\n\x01y\x18\x03 \x02(\x02\x12\x13\n\x08quantity\x18\x04 \x02(\x02:\x01\x30\x12\x13\n\x0bwindowStart\x18\x05 \x01(\x02\x12\x11\n\twindowEnd\x18\x06 \x01(\x02\"\xfd\x01\n\x06\x43VRPTW\x12\x1f\n\x06points\x18\x01 \x03(\x0b\x32\x0f.CVRPTW.Geocode\x12\x1e\n\x05\x64\x65pot\x18\x02 \x02(\x0b\x32\x0f.CVRPTW.Geocode\x12\x18\n\x10NumberOfVehicles\x18\x03 \x02(\x05\x12\x17\n\x0fVehicleCapacity\x18\x04 \x02(\x02\x12?\n\x0c\x64istancetype\x18\x05 \x01(\x0e\x32\x1c.CVRPTW.CVRPTW.eDistanceType:\x0bRoadNetwork\">\n\reDistanceType\x12\x0f\n\x0bRoadNetwork\x10\x01\x12\r\n\tEuclidean\x10\x02\x12\r\n\tHaversine\x10\x03\"\xcb\x01\n\x0cSolveRequest\x12\x1d\n\x05model\x18\x01 \x01(\x0b\x32\x0e.CVRPTW.CVRPTW\x12\x0f\n\x07modelID\x18\x02 \x01(\t\x12\x15\n\rvisitSequence\x18\x03 \x03(\t\x12;\n\tsolveType\x18\x04 \x01(\x0e\x32\x1e.CVRPTW.SolveRequest.SolveType:\x08Optimise\"7\n\tSolveType\x12\x0c\n\x08Optimise\x10\x00\x12\x0c\n\x08\x45valuate\x10\x01\x12\x0e\n\nReOptimise\x10\x02\"}\n\x04\x45\x64ge\x12\x0c\n\x04\x66rom\x18\x01 \x02(\t\x12\n\n\x02to\x18\x02 \x02(\t\x12\x10\n\x08\x64istance\x18\x03 \x01(\x02\x12\'\n\x08geometry\x18\x05 \x03(\x0b\x32\x15.CVRPTW.Edge.Geometry\x1a \n\x08Geometry\x12\t\n\x01x\x18\x01 \x02(\x02\x12\t\n\x01y\x18\x02 \x02(\x02\"\xbc\x01\n\x10SolutionResponse\x12.\n\x06routes\x18\x01 \x03(\x0b\x32\x1e.CVRPTW.SolutionResponse.Route\x12\x11\n\tobjective\x18\x02 \x02(\x02\x1a\x65\n\x05Route\x12\x10\n\x08sequence\x18\x01 \x03(\t\x12\x1b\n\x05\x65\x64ges\x18\x02 \x03(\x0b\x32\x0c.CVRPTW.Edge\x12\x17\n\x0fvisitCapacities\x18\x03 \x03(\x02\x12\x14\n\x0c\x61rrivalTimes\x18\x04 \x03(\x02')
)



_CVRPTW_EDISTANCETYPE = _descriptor.EnumDescriptor(
  name='eDistanceType',
  full_name='CVRPTW.CVRPTW.eDistanceType',
  filename=None,
  file=DESCRIPTOR,
  values=[
    _descriptor.EnumValueDescriptor(
      name='RoadNetwork', index=0, number=1,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='Euclidean', index=1, number=2,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='Haversine', index=2, number=3,
      serialized_options=None,
      type=None),
  ],
  containing_type=None,
  serialized_options=None,
  serialized_start=335,
  serialized_end=397,
)
_sym_db.RegisterEnumDescriptor(_CVRPTW_EDISTANCETYPE)

_SOLVEREQUEST_SOLVETYPE = _descriptor.EnumDescriptor(
  name='SolveType',
  full_name='CVRPTW.SolveRequest.SolveType',
  filename=None,
  file=DESCRIPTOR,
  values=[
    _descriptor.EnumValueDescriptor(
      name='Optimise', index=0, number=0,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='Evaluate', index=1, number=1,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='ReOptimise', index=2, number=2,
      serialized_options=None,
      type=None),
  ],
  containing_type=None,
  serialized_options=None,
  serialized_start=548,
  serialized_end=603,
)
_sym_db.RegisterEnumDescriptor(_SOLVEREQUEST_SOLVETYPE)


_GEOCODE = _descriptor.Descriptor(
  name='Geocode',
  full_name='CVRPTW.Geocode',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='id', full_name='CVRPTW.Geocode.id', index=0,
      number=1, type=9, cpp_type=9, label=2,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='x', full_name='CVRPTW.Geocode.x', index=1,
      number=2, type=2, cpp_type=6, label=2,
      has_default_value=False, default_value=float(0),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='y', full_name='CVRPTW.Geocode.y', index=2,
      number=3, type=2, cpp_type=6, label=2,
      has_default_value=False, default_value=float(0),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='quantity', full_name='CVRPTW.Geocode.quantity', index=3,
      number=4, type=2, cpp_type=6, label=2,
      has_default_value=True, default_value=float(0),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='windowStart', full_name='CVRPTW.Geocode.windowStart', index=4,
      number=5, type=2, cpp_type=6, label=1,
      has_default_value=False, default_value=float(0),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='windowEnd', full_name='CVRPTW.Geocode.windowEnd', index=5,
      number=6, type=2, cpp_type=6, label=1,
      has_default_value=False, default_value=float(0),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto2',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=37,
  serialized_end=141,
)


_CVRPTW = _descriptor.Descriptor(
  name='CVRPTW',
  full_name='CVRPTW.CVRPTW',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='points', full_name='CVRPTW.CVRPTW.points', index=0,
      number=1, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='depot', full_name='CVRPTW.CVRPTW.depot', index=1,
      number=2, type=11, cpp_type=10, label=2,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='NumberOfVehicles', full_name='CVRPTW.CVRPTW.NumberOfVehicles', index=2,
      number=3, type=5, cpp_type=1, label=2,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='VehicleCapacity', full_name='CVRPTW.CVRPTW.VehicleCapacity', index=3,
      number=4, type=2, cpp_type=6, label=2,
      has_default_value=False, default_value=float(0),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='distancetype', full_name='CVRPTW.CVRPTW.distancetype', index=4,
      number=5, type=14, cpp_type=8, label=1,
      has_default_value=True, default_value=1,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
    _CVRPTW_EDISTANCETYPE,
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto2',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=144,
  serialized_end=397,
)


_SOLVEREQUEST = _descriptor.Descriptor(
  name='SolveRequest',
  full_name='CVRPTW.SolveRequest',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='model', full_name='CVRPTW.SolveRequest.model', index=0,
      number=1, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='modelID', full_name='CVRPTW.SolveRequest.modelID', index=1,
      number=2, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='visitSequence', full_name='CVRPTW.SolveRequest.visitSequence', index=2,
      number=3, type=9, cpp_type=9, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='solveType', full_name='CVRPTW.SolveRequest.solveType', index=3,
      number=4, type=14, cpp_type=8, label=1,
      has_default_value=True, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
    _SOLVEREQUEST_SOLVETYPE,
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto2',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=400,
  serialized_end=603,
)


_EDGE_GEOMETRY = _descriptor.Descriptor(
  name='Geometry',
  full_name='CVRPTW.Edge.Geometry',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='x', full_name='CVRPTW.Edge.Geometry.x', index=0,
      number=1, type=2, cpp_type=6, label=2,
      has_default_value=False, default_value=float(0),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='y', full_name='CVRPTW.Edge.Geometry.y', index=1,
      number=2, type=2, cpp_type=6, label=2,
      has_default_value=False, default_value=float(0),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto2',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=698,
  serialized_end=730,
)

_EDGE = _descriptor.Descriptor(
  name='Edge',
  full_name='CVRPTW.Edge',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='from', full_name='CVRPTW.Edge.from', index=0,
      number=1, type=9, cpp_type=9, label=2,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='to', full_name='CVRPTW.Edge.to', index=1,
      number=2, type=9, cpp_type=9, label=2,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='distance', full_name='CVRPTW.Edge.distance', index=2,
      number=3, type=2, cpp_type=6, label=1,
      has_default_value=False, default_value=float(0),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='geometry', full_name='CVRPTW.Edge.geometry', index=3,
      number=5, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[_EDGE_GEOMETRY, ],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto2',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=605,
  serialized_end=730,
)


_SOLUTIONRESPONSE_ROUTE = _descriptor.Descriptor(
  name='Route',
  full_name='CVRPTW.SolutionResponse.Route',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='sequence', full_name='CVRPTW.SolutionResponse.Route.sequence', index=0,
      number=1, type=9, cpp_type=9, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='edges', full_name='CVRPTW.SolutionResponse.Route.edges', index=1,
      number=2, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='visitCapacities', full_name='CVRPTW.SolutionResponse.Route.visitCapacities', index=2,
      number=3, type=2, cpp_type=6, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='arrivalTimes', full_name='CVRPTW.SolutionResponse.Route.arrivalTimes', index=3,
      number=4, type=2, cpp_type=6, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto2',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=820,
  serialized_end=921,
)

_SOLUTIONRESPONSE = _descriptor.Descriptor(
  name='SolutionResponse',
  full_name='CVRPTW.SolutionResponse',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='routes', full_name='CVRPTW.SolutionResponse.routes', index=0,
      number=1, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='objective', full_name='CVRPTW.SolutionResponse.objective', index=1,
      number=2, type=2, cpp_type=6, label=2,
      has_default_value=False, default_value=float(0),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[_SOLUTIONRESPONSE_ROUTE, ],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto2',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=733,
  serialized_end=921,
)

_CVRPTW.fields_by_name['points'].message_type = _GEOCODE
_CVRPTW.fields_by_name['depot'].message_type = _GEOCODE
_CVRPTW.fields_by_name['distancetype'].enum_type = _CVRPTW_EDISTANCETYPE
_CVRPTW_EDISTANCETYPE.containing_type = _CVRPTW
_SOLVEREQUEST.fields_by_name['model'].message_type = _CVRPTW
_SOLVEREQUEST.fields_by_name['solveType'].enum_type = _SOLVEREQUEST_SOLVETYPE
_SOLVEREQUEST_SOLVETYPE.containing_type = _SOLVEREQUEST
_EDGE_GEOMETRY.containing_type = _EDGE
_EDGE.fields_by_name['geometry'].message_type = _EDGE_GEOMETRY
_SOLUTIONRESPONSE_ROUTE.fields_by_name['edges'].message_type = _EDGE
_SOLUTIONRESPONSE_ROUTE.containing_type = _SOLUTIONRESPONSE
_SOLUTIONRESPONSE.fields_by_name['routes'].message_type = _SOLUTIONRESPONSE_ROUTE
DESCRIPTOR.message_types_by_name['Geocode'] = _GEOCODE
DESCRIPTOR.message_types_by_name['CVRPTW'] = _CVRPTW
DESCRIPTOR.message_types_by_name['SolveRequest'] = _SOLVEREQUEST
DESCRIPTOR.message_types_by_name['Edge'] = _EDGE
DESCRIPTOR.message_types_by_name['SolutionResponse'] = _SOLUTIONRESPONSE
_sym_db.RegisterFileDescriptor(DESCRIPTOR)

Geocode = _reflection.GeneratedProtocolMessageType('Geocode', (_message.Message,), dict(
  DESCRIPTOR = _GEOCODE,
  __module__ = 'cvrptw_acyas3nzweqb_pb2'
  # @@protoc_insertion_point(class_scope:CVRPTW.Geocode)
  ))
_sym_db.RegisterMessage(Geocode)

CVRPTW = _reflection.GeneratedProtocolMessageType('CVRPTW', (_message.Message,), dict(
  DESCRIPTOR = _CVRPTW,
  __module__ = 'cvrptw_acyas3nzweqb_pb2'
  # @@protoc_insertion_point(class_scope:CVRPTW.CVRPTW)
  ))
_sym_db.RegisterMessage(CVRPTW)

SolveRequest = _reflection.GeneratedProtocolMessageType('SolveRequest', (_message.Message,), dict(
  DESCRIPTOR = _SOLVEREQUEST,
  __module__ = 'cvrptw_acyas3nzweqb_pb2'
  # @@protoc_insertion_point(class_scope:CVRPTW.SolveRequest)
  ))
_sym_db.RegisterMessage(SolveRequest)

Edge = _reflection.GeneratedProtocolMessageType('Edge', (_message.Message,), dict(

  Geometry = _reflection.GeneratedProtocolMessageType('Geometry', (_message.Message,), dict(
    DESCRIPTOR = _EDGE_GEOMETRY,
    __module__ = 'cvrptw_acyas3nzweqb_pb2'
    # @@protoc_insertion_point(class_scope:CVRPTW.Edge.Geometry)
    ))
  ,
  DESCRIPTOR = _EDGE,
  __module__ = 'cvrptw_acyas3nzweqb_pb2'
  # @@protoc_insertion_point(class_scope:CVRPTW.Edge)
  ))
_sym_db.RegisterMessage(Edge)
_sym_db.RegisterMessage(Edge.Geometry)

SolutionResponse = _reflection.GeneratedProtocolMessageType('SolutionResponse', (_message.Message,), dict(

  Route = _reflection.GeneratedProtocolMessageType('Route', (_message.Message,), dict(
    DESCRIPTOR = _SOLUTIONRESPONSE_ROUTE,
    __module__ = 'cvrptw_acyas3nzweqb_pb2'
    # @@protoc_insertion_point(class_scope:CVRPTW.SolutionResponse.Route)
    ))
  ,
  DESCRIPTOR = _SOLUTIONRESPONSE,
  __module__ = 'cvrptw_acyas3nzweqb_pb2'
  # @@protoc_insertion_point(class_scope:CVRPTW.SolutionResponse)
  ))
_sym_db.RegisterMessage(SolutionResponse)
_sym_db.RegisterMessage(SolutionResponse.Route)


# @@protoc_insertion_point(module_scope)
