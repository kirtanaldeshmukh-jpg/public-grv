import React, { useState } from 'react';
import {
  Building2,
  Users,
  Briefcase,
  ShieldCheck,
  Search,
  Plus,
  Mail,
  Phone,
} from 'lucide-react';
import { mockDepartments, mockOfficers } from '../mocks';
import apiClient from '../api/client';

export const DepartmentsPage: React.FC = () => {
  const [departments, setDepartments] = useState<any[]>(mockDepartments);
  const [officers, setOfficers] = useState<any[]>(mockOfficers);
  const [selectedDeptId, setSelectedDeptId] = useState<number | 'ALL'>('ALL');
  const [searchTerm, setSearchTerm] = useState('');

  React.useEffect(() => {
    Promise.allSettled([
      apiClient.get('/departments'),
      apiClient.get('/officers'),
    ]).then(([deptRes, offRes]) => {
      if (deptRes.status === 'fulfilled' && deptRes.value.data?.data) {
        setDepartments(deptRes.value.data.data);
      }
      if (offRes.status === 'fulfilled' && offRes.value.data?.data) {
        setOfficers(offRes.value.data.data);
      }
    });
  }, []);

  const filteredOfficers = officers.filter((off) => {
    const matchesSearch =
      off.fullName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      off.employeeCode.toLowerCase().includes(searchTerm.toLowerCase()) ||
      off.designation.toLowerCase().includes(searchTerm.toLowerCase()) ||
      off.departmentNameEn.toLowerCase().includes(searchTerm.toLowerCase());

    if (!matchesSearch) return false;
    if (selectedDeptId !== 'ALL' && off.departmentId !== selectedDeptId) return false;
    return true;
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <section className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <span className="px-2.5 py-0.5 rounded-full bg-indigo-50 text-primary text-[11px] font-bold uppercase tracking-wider">
              SAKALA Statutory Roster
            </span>
            <span className="text-xs text-gray-500">Zone-04 Command</span>
          </div>

          <h1 className="text-2xl sm:text-3xl font-extrabold text-gray-900 tracking-tight">
            Departments & Officers Management
          </h1>
          <p className="text-xs text-gray-600 leading-relaxed">
            Jurisdictional authority directory, administrative roster, and caseload allocation under Karnataka SAKALA Services Act.
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <button
            type="button"
            onClick={() => alert('New department registration modal')}
            className="px-4 py-2.5 rounded-xl bg-white border border-gray-300 text-gray-700 text-xs font-bold hover:bg-gray-50 flex items-center gap-1.5 transition-colors shadow-2xs"
          >
            <Building2 className="w-4 h-4 text-primary" />
            <span>+ Add Department</span>
          </button>
          <button
            type="button"
            onClick={() => alert('New officer onboarding modal')}
            className="px-4 py-2.5 rounded-xl bg-primary text-white text-xs font-bold hover:bg-primary-hover flex items-center gap-1.5 transition-colors shadow-2xs"
          >
            <Plus className="w-4 h-4" />
            <span>+ Add Officer</span>
          </button>
        </div>
      </section>

      {/* 4 Quick KPI Stats */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-white border border-gray-200 rounded-2xl p-5 shadow-xs flex flex-col justify-between">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-bold text-gray-400 uppercase tracking-wider">Civic Departments</span>
            <div className="w-8 h-8 rounded-lg bg-indigo-50 text-primary flex items-center justify-center">
              <Building2 className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-2xl font-extrabold text-gray-900">{departments.length}</span>
            <span className="text-xs text-teal font-bold">100% Active</span>
          </div>
          <span className="text-[10px] text-gray-400 mt-1">Full jurisdictional coverage</span>
        </div>

        <div className="bg-white border border-gray-200 rounded-2xl p-5 shadow-xs flex flex-col justify-between">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-bold text-gray-400 uppercase tracking-wider">Onboarded Officers</span>
            <div className="w-8 h-8 rounded-lg bg-teal/10 text-teal flex items-center justify-center">
              <Users className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-2xl font-extrabold text-gray-900">142</span>
            <span className="text-xs text-gray-500 font-semibold">Statutory</span>
          </div>
          <span className="text-[10px] text-gray-400 mt-1">18 Sup • 8 HoD • 116 Field</span>
        </div>

        <div className="bg-white border border-gray-200 rounded-2xl p-5 shadow-xs flex flex-col justify-between">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-bold text-gray-400 uppercase tracking-wider">Average Caseload</span>
            <div className="w-8 h-8 rounded-lg bg-amber-50 text-amber-600 flex items-center justify-center">
              <Briefcase className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-2xl font-extrabold text-gray-900">6.4</span>
            <span className="text-xs text-gray-500">Dockets / Officer</span>
          </div>
          <div className="w-full bg-gray-100 h-1.5 rounded-full overflow-hidden mt-2">
            <div className="bg-amber-500 h-full rounded-full" style={{ width: '58%' }}></div>
          </div>
        </div>

        <div className="bg-white border border-gray-200 rounded-2xl p-5 shadow-xs flex flex-col justify-between">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-bold text-gray-400 uppercase tracking-wider">SAKALA Benchmark</span>
            <div className="w-8 h-8 rounded-lg bg-indigo-50 text-primary flex items-center justify-center">
              <ShieldCheck className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-2xl font-extrabold text-teal">92.4%</span>
            <span className="text-xs text-teal font-semibold">Target: 90%</span>
          </div>
          <span className="text-[10px] text-teal font-semibold mt-1">State SLA Compliant</span>
        </div>
      </div>

      {/* Department Cards Grid */}
      <section className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-base font-bold text-gray-900">Civic Authorities Directory</h2>
          <span className="text-xs text-gray-500">Click a card to filter officer roster</span>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-5 gap-4">
          {departments.map((dept) => {
            const isSelected = selectedDeptId === dept.id;
            return (
              <div
                key={dept.id}
                onClick={() => setSelectedDeptId(isSelected ? 'ALL' : dept.id)}
                className={`bg-white border rounded-2xl p-5 shadow-xs cursor-pointer transition-all flex flex-col justify-between ${
                  isSelected
                    ? 'border-primary ring-2 ring-primary/20 shadow-md'
                    : 'border-gray-200 hover:border-primary/50'
                }`}
              >
                <div className="space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="px-2 py-0.5 rounded-md bg-indigo-50 text-primary font-mono text-[11px] font-bold">
                      {dept.code}
                    </span>
                    <span className="text-[10px] font-bold text-teal bg-teal/10 px-2 py-0.5 rounded-full">
                      {dept.slaCompliance}% SLA
                    </span>
                  </div>

                  <div>
                    <h3 className="text-sm font-bold text-gray-900 leading-snug">{dept.nameEn}</h3>
                    <span className="text-[11px] text-gray-400 block">{dept.nameKn}</span>
                  </div>

                  <p className="text-[11px] text-gray-600 line-clamp-2 leading-relaxed">
                    {dept.description}
                  </p>
                </div>

                <div className="mt-4 pt-3 border-t border-gray-100 flex items-center justify-between text-[11px] text-gray-500">
                  <span>{dept.totalOfficers} Officers</span>
                  <span>{dept.defaultSlaDays} Days SLA</span>
                </div>
              </div>
            );
          })}
        </div>
      </section>

      {/* Officer Roster Table */}
      <section className="bg-white border border-gray-200 rounded-2xl shadow-xs overflow-hidden">
        <div className="p-5 border-b border-gray-200 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h2 className="text-base font-bold text-gray-900">Assigned Officers & Field Inspectors Roster</h2>
            <p className="text-xs text-gray-500">
              {selectedDeptId !== 'ALL'
                ? `Filtered by selected department • ${filteredOfficers.length} Officers`
                : `All Active Officers • ${filteredOfficers.length} Officers`}
            </p>
          </div>

          <div className="flex items-center gap-3">
            <div className="relative">
              <Search className="w-4 h-4 text-gray-400 absolute left-3 top-2.5" />
              <input
                type="text"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                placeholder="Search officer name, code..."
                className="bg-gray-50 border border-gray-200 rounded-xl pl-9 pr-3 py-1.5 text-xs text-gray-900 outline-none focus:bg-white focus:border-primary transition-colors w-60"
              />
            </div>
            {selectedDeptId !== 'ALL' && (
              <button
                type="button"
                onClick={() => setSelectedDeptId('ALL')}
                className="text-xs text-primary font-bold hover:underline"
              >
                Clear Filter
              </button>
            )}
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-gray-50/75 border-b border-gray-200 text-[11px] font-bold text-gray-500 uppercase tracking-wider">
                <th className="py-3 px-4">Officer Particulars</th>
                <th className="py-3 px-4">Department & Designation</th>
                <th className="py-3 px-4">Official Contact</th>
                <th className="py-3 px-4">Caseload</th>
                <th className="py-3 px-4">Redressal Track</th>
                <th className="py-3 px-4 text-right">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 text-xs">
              {filteredOfficers.map((off) => (
                <tr key={off.id} className="hover:bg-indigo-50/30 transition-colors">
                  <td className="py-3.5 px-4">
                    <span className="font-bold text-gray-900 block">{off.fullName}</span>
                    <span className="font-mono text-[10px] text-gray-400">{off.employeeCode}</span>
                  </td>

                  <td className="py-3.5 px-4">
                    <span className="font-semibold text-gray-800 block">{off.departmentNameEn}</span>
                    <span className="text-[11px] text-gray-500">{off.designation}</span>
                  </td>

                  <td className="py-3.5 px-4 space-y-0.5">
                    <div className="flex items-center gap-1.5 text-gray-600 text-[11px]">
                      <Phone className="w-3 h-3 text-gray-400" />
                      <span>{off.phoneNumber}</span>
                    </div>
                    <div className="flex items-center gap-1.5 text-gray-600 text-[11px] truncate">
                      <Mail className="w-3 h-3 text-gray-400" />
                      <span className="truncate">{off.email}</span>
                    </div>
                  </td>

                  <td className="py-3.5 px-4">
                    <span className="font-extrabold text-primary text-sm block">{off.activeCases}</span>
                    <span className="text-[10px] text-gray-400">Active Grievances</span>
                  </td>

                  <td className="py-3.5 px-4">
                    <span className="font-bold text-teal block">{off.resolvedCases} Resolved</span>
                    <span className="text-[10px] text-gray-400">98.2% on-time</span>
                  </td>

                  <td className="py-3.5 px-4 text-right">
                    <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-teal/10 text-teal text-[10px] font-bold">
                      <span className="w-1.5 h-1.5 rounded-full bg-teal"></span>
                      On Duty
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
};
