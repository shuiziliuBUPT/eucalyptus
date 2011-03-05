/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.util.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.log4j.Logger;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.Assertions;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.eucalyptus.util.async.Futures;

public class ExecPair<V> implements Runnable {
  private static Logger                    LOG    = Logger.getLogger( ExecPair.class );
  private Callable<V>                callable;
  private Runnable                   runnable;
  private final CheckedListenableFuture<V> future = Futures.newGenericFuture( );
  private final ExecutorService            executor;
  
  ExecPair( Callable callable, ExecutorService executor ) {
    Assertions.assertNotNull( callable, "Callable was null." );
    Assertions.assertNotNull( executor, "ExecutorService was null." );
    
    this.callable = callable;
    this.executor = executor;
  }
  
  ExecPair( final Runnable runnable, ExecutorService executor ) {
    Assertions.assertNotNull( runnable, "Runnable was null." );
    Assertions.assertNotNull( executor, "ExecutorService was null." );

    this.runnable = runnable;
    this.executor = executor;
  }
  
  @Override
  public void run( ) {
    try {
      if( this.runnable != null ) {
        EventRecord.here( runnable.getClass( ), EventType.FUTURE, "run(" + runnable.toString( ) + ")" ).debug( );
        this.executor.submit( this.runnable, null ).get( );
        this.future.set( null );
      } else {
        EventRecord.here( callable.getClass( ), EventType.FUTURE, "call(" + callable.toString( ) + ")" ).debug( );
        this.future.set( this.executor.submit( callable ).get( ) );
      }
    } catch ( InterruptedException ex ) {
      LOG.error( ex, ex );
      this.future.setException( ex );
    } catch ( ExecutionException ex ) {
      LOG.error( ex, ex );
      this.future.setException( ex.getCause( ) );
    } catch ( Throwable ex ) {
      LOG.error( ex, ex );
      this.future.setException( ex.getCause( ) );
    }
  }
  
  CheckedListenableFuture<V> getFuture( ) {
    return this.future;
  }
  
  protected ExecutorService getExecutor( ) {
    return this.executor;
  }

  @Override
  public String toString( ) {
    return String.format( "ExecPair:callable=%s:runnable=%s", this.callable, this.runnable );
  }
  

}