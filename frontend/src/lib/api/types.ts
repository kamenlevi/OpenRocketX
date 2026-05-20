// Wire types — must stay in sync with the Java records in x.model.

export type ShapeType =
  | 'CONICAL'
  | 'OGIVE'
  | 'ELLIPSOID'
  | 'PARABOLIC'
  | 'POWER'
  | 'HAACK';

export interface NoseConeSpec {
  shape: ShapeType;
  length: number;
  baseRadius: number;
  shapeParam: number;
  wallThickness: number;
}

export interface BodyTubeSpec {
  length: number;
  outerRadius: number;
  innerRadius: number;
}

export interface TransitionSpec {
  shape: ShapeType;
  length: number;
  foreRadius: number;
  aftRadius: number;
  shapeParam: number;
}

export interface FinSetSpec {
  count: number;
  thickness: number;
  cantAngleRad: number;
  finPoints: [number, number][];
  mountRadius: number;
}

export type BodySection =
  | { kind: 'nose'; spec: NoseConeSpec }
  | { kind: 'body'; spec: BodyTubeSpec }
  | { kind: 'transition'; spec: TransitionSpec };

export interface MountedFinSet {
  spec: FinSetSpec;
  xOffset: number;
}

export interface Stage {
  name: string;
  sections: BodySection[];
  fins: MountedFinSet[];
}

export interface RocketSpec {
  name: string;
  stages: Stage[];
}

export interface PreviewMesh {
  positions: number[];
  indices: number[];
  manifold: boolean;
}

export interface Motor {
  id: string;
  manufacturer: string;
  designation: string;
  delay: string;
  diameter: number;
  length: number;
  totalImpulseNs: number;
  averageThrustN: number;
  burnTimeS: number;
  impulseClass: string;
}

export interface MotorList {
  motors: Motor[];
  stub?: boolean;
  note?: string;
}

export interface SimSample {
  t: number;
  altitude: number;
  speed: number;
  velocity: [number, number, number];
  acceleration: number;
}

export interface SimResult {
  samples: SimSample[];
  summary: {
    apogeeM: number;
    maxVelMs: number;
    burnoutS: number;
    flightTimeS: number;
  };
}
