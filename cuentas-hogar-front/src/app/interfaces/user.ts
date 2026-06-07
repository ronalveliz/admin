import { RolName } from "./rolName";

export interface User {
    id:number;
    nombre: string;
    email: string;
    password: string;
    phone: string;
    rolname: RolName;
    imgUser: string;
}
