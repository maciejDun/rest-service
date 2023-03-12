package com.code.block.rest_service.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class User {
  String id;
  String login;
  String password;
}
