/*
 * Copyright (c) 2015, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#ifndef SHARE_GC_Z_C1_ZBARRIERSETC1_HPP
#define SHARE_GC_Z_C1_ZBARRIERSETC1_HPP

#include "c1/c1_CodeStubs.hpp"
#include "c1/c1_IR.hpp"
#include "c1/c1_LIR.hpp"
#include "gc/shared/c1/barrierSetC1.hpp"
#include "oops/accessDecorators.hpp"

class ZLoadBarrierStubC1 : public CodeStub {
private:
  DecoratorSet _decorators;
  LIR_Opr      _ref_addr;
  LIR_Opr      _ref;
  LIR_Opr      _tmp;
  address      _runtime_stub;

public:
  ZLoadBarrierStubC1(LIRAccess& access, LIR_Opr ref, address runtime_stub);

  DecoratorSet decorators() const;
  LIR_Opr ref() const;
  LIR_Opr ref_addr() const;
  LIR_Opr tmp() const;
  address runtime_stub() const;

  virtual void emit_code(LIR_Assembler* ce);
  virtual void visit(LIR_OpVisitState* visitor);

#ifndef PRODUCT
  virtual void print_name(outputStream* out) const;
#endif // PRODUCT
};

class ZStoreBarrierStubC1 : public CodeStub {
private:
  LIR_Opr _ref_addr;
  LIR_Opr _new_zaddress;
  LIR_Opr _new_zpointer;
  LIR_Opr _tmp;
  bool    _is_atomic;
  address _runtime_stub;

public:
  ZStoreBarrierStubC1(LIRAccess& access,
                      LIR_Opr new_zaddress,
                      LIR_Opr new_zpointer,
                      LIR_Opr tmp,
                      bool is_atomic,
                      address runtime_stub);

  LIR_Opr ref_addr() const;
  LIR_Opr new_zaddress() const;
  LIR_Opr new_zpointer() const;
  LIR_Opr tmp() const;
  bool is_atomic() const;
  address runtime_stub() const;

  virtual void emit_code(LIR_Assembler* ce);
  virtual void visit(LIR_OpVisitState* visitor);

#ifndef PRODUCT
  virtual void print_name(outputStream* out) const;
#endif // PRODUCT
};

class ZBarrierSetC1 : public BarrierSetC1 {
private:
  address _load_barrier_on_oop_field_preloaded_runtime_stub;
  address _load_barrier_on_weak_oop_field_preloaded_runtime_stub;
  address _store_barrier_on_oop_field_with_healing;
  address _store_barrier_on_oop_field_without_healing;

  address load_barrier_on_oop_field_preloaded_runtime_stub(DecoratorSet decorators) const;
  address store_barrier_on_oop_field_runtime_stub(bool self_healing) const;

  LIR_Opr color(LIRAccess& access, LIR_Opr ref) const;

  void load_barrier(LIRAccess& access, LIR_Opr result) const;
  LIR_Opr store_barrier(LIRAccess& access, LIR_Opr new_zaddress, bool is_atomic) const;

protected:
  virtual LIR_Opr resolve_address(LIRAccess& access, bool resolve_in_register);
  virtual void load_at_resolved(LIRAccess& access, LIR_Opr result);
  virtual void store_at_resolved(LIRAccess& access, LIR_Opr value);
  virtual LIR_Opr atomic_cmpxchg_at_resolved(LIRAccess& access, LIRItem& cmp_value, LIRItem& new_value);
  virtual LIR_Opr atomic_xchg_at_resolved(LIRAccess& access, LIRItem& value);

public:
  ZBarrierSetC1();

  virtual bool generate_c1_runtime_stubs(BufferBlob* blob);
};

#endif // SHARE_GC_Z_C1_ZBARRIERSETC1_HPP
