import type { RocketSpec } from './api/types.js';

/** Single-stage demo rocket used as a starter design in the UI. */
export function exampleRocket(): RocketSpec {
  return {
    name: 'demo',
    stages: [
      {
        name: 'core',
        sections: [
          {
            kind: 'nose',
            spec: {
              shape: 'OGIVE',
              length: 0.12,
              baseRadius: 0.025,
              shapeParam: 1.0,
              wallThickness: 0.0015
            }
          },
          {
            kind: 'body',
            spec: { length: 0.50, outerRadius: 0.025, innerRadius: 0.024 }
          }
        ],
        fins: [
          {
            spec: {
              count: 3,
              thickness: 0.003,
              cantAngleRad: 0,
              finPoints: [
                [0, 0],
                [0.10, 0],
                [0.07, 0.06],
                [0.03, 0.06]
              ],
              mountRadius: 0.025
            },
            xOffset: 0.50
          }
        ]
      }
    ]
  };
}
