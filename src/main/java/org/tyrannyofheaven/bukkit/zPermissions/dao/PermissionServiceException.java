/*
 * Copyright 2011 ZerothAngel <zerothangel@tyrannyofheaven.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tyrannyofheaven.bukkit.zPermissions.dao;

/**
 * Thrown for any errors originating within PermissionService.
 * 
 * @author zerothangel
 */
public class PermissionServiceException extends RuntimeException {

    private static final long serialVersionUID = 79973002335863673L;

    public PermissionServiceException() {
    }

    public PermissionServiceException(String message) {
        super(message);
    }

    public PermissionServiceException(Throwable cause) {
        super(cause);
    }

    public PermissionServiceException(String message, Throwable cause) {
        super(message, cause);
    }

}
